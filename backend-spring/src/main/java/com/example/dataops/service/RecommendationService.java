package com.example.dataops.service;

import com.example.dataops.dto.AiDtos;
import com.example.dataops.dto.RecommendationDtos;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.Agency;
import com.example.dataops.model.Alert;
import com.example.dataops.model.AlertSeverity;
import com.example.dataops.model.HistoriqueModule;
import com.example.dataops.model.Product;
import com.example.dataops.model.Recommendation;
import com.example.dataops.model.RecommendationModuleSource;
import com.example.dataops.model.RecommendationPriority;
import com.example.dataops.model.RecommendationStatus;
import com.example.dataops.model.RecommendationType;
import com.example.dataops.model.Sale;
import com.example.dataops.repository.AlertRepository;
import com.example.dataops.repository.RecommendationRepository;
import com.example.dataops.repository.SaleRepository;
import com.example.dataops.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RecommendationService {
    private static final Pattern PRODUCT_AGENCY_PATTERN = Pattern.compile("(?i)produit\\s+([A-Z0-9._-]+)\\s*/\\s*agence\\s+([A-Z0-9._-]+)");

    private final RecommendationRepository recommendationRepository;
    private final AlertRepository alertRepository;
    private final SaleRepository saleRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AiAnalysisService aiAnalysisService;
    private final ProductService productService;
    private final AgencyService agencyService;
    private final DataopsMapper mapper;
    private final BlockchainService blockchainService;
    private final RegleMetierService regleMetierService;
    private final HistoriqueActionService historiqueActionService;

    public RecommendationService(
        RecommendationRepository recommendationRepository,
        AlertRepository alertRepository,
        SaleRepository saleRepository,
        StockMovementRepository stockMovementRepository,
        AiAnalysisService aiAnalysisService,
        ProductService productService,
        AgencyService agencyService,
        DataopsMapper mapper,
        BlockchainService blockchainService,
        RegleMetierService regleMetierService,
        HistoriqueActionService historiqueActionService
    ) {
        this.recommendationRepository = recommendationRepository;
        this.alertRepository = alertRepository;
        this.saleRepository = saleRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.aiAnalysisService = aiAnalysisService;
        this.productService = productService;
        this.agencyService = agencyService;
        this.mapper = mapper;
        this.blockchainService = blockchainService;
        this.regleMetierService = regleMetierService;
        this.historiqueActionService = historiqueActionService;
    }

    @Transactional
    public List<RecommendationDtos.RecommendationResponse> findAll() {
        ensureSynthesisRecommendations();
        return recommendationRepository.findAllByOrderByCreatedAtDesc().stream()
            .sorted(Comparator.comparing(this::priorityRank).thenComparing(Recommendation::getCreatedAt).reversed())
            .map(mapper::toRecommendationResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public RecommendationDtos.RecommendationResponse findById(Long id) {
        return mapper.toRecommendationResponse(getEntity(id));
    }

    @Transactional
    public RecommendationDtos.RecommendationResponse updateStatus(Long id, RecommendationStatus status) {
        Recommendation recommendation = getEntity(id);
        RecommendationStatus previous = recommendation.getStatus();
        recommendation.setStatus(status);
        blockchainService.append("RECOMMENDATION_STATUS_UPDATED", "system", "recommendationId=" + id + "|status=" + status);
        historiqueActionService.enregistrerAction(
            "DECISION_RECOMMANDATION",
            toHistoriqueModule(recommendation.getModuleSource()),
            "Decision sur recommandation : " + recommendation.getMessage(),
            previous.name(),
            status.name(),
            String.valueOf(id),
            null
        );
        return mapper.toRecommendationResponse(recommendation);
    }

    @Transactional
    public RecommendationDtos.RecommendationResponse validate(Long id) {
        return updateStatus(id, RecommendationStatus.VALIDEE);
    }

    @Transactional
    public RecommendationDtos.RecommendationResponse reject(Long id) {
        return updateStatus(id, RecommendationStatus.REJETEE);
    }

    @Transactional
    public RecommendationDtos.RecommendationGenerateResponse generate() {
        Set<String> existingKeys = existingKeys();
        List<Recommendation> created = new ArrayList<>();

        generateFromExistingAlerts(existingKeys, created);
        generateFromStockPredictions(existingKeys, created);
        generateFromCriticalStocks(existingKeys, created);
        generateFromSalesAnomalies(existingKeys, created);
        generateFromDormantStocks(existingKeys, created);

        if (!created.isEmpty()) {
            blockchainService.append("RECOMMENDATIONS_GENERATED", "system", "createdCount=" + created.size());
        }

        return new RecommendationDtos.RecommendationGenerateResponse(
            created.size(),
            findAll()
        );
    }

    private void generateFromExistingAlerts(Set<String> existingKeys, List<Recommendation> created) {
        for (Alert alert : alertRepository.findByResolvedFalseOrderByCreatedAtDesc()) {
            ProductAgency match = extractProductAgency(alert.getMessage());
            createIfNew(
                existingKeys,
                created,
                RecommendationType.ALERTE_INTELLIGENTE,
                RecommendationModuleSource.ALERTES,
                alert.getSeverity(),
                "Alerte IA a traiter : " + alert.getTitle(),
                actionFromAlert(alert.getMessage()),
                "Réduction du délai de réaction sur situation critique.",
                match.agency().orElse(null),
                match.product().orElse(null),
                alert
            );
        }
    }

    private void generateFromStockPredictions(Set<String> existingKeys, List<Recommendation> created) {
        AiDtos.StockPredictionAnalysisResponse predictions = aiAnalysisService.analyzeStockPredictions();
        long purchaseAlertThreshold = regleMetierService.getDecimal(RegleMetierService.ALERTE_ACHAT_SEUIL, BigDecimal.valueOf(5)).longValue();
        for (AiDtos.StockPredictionResponse prediction : predictions.results()) {
            if (prediction.predictedDaysToStockout() == null || prediction.predictedDaysToStockout() > purchaseAlertThreshold) {
                continue;
            }
            Product product = resolveProduct(prediction.productCode()).orElse(null);
            Agency agency = resolveAgency(prediction.agencyCode()).orElse(null);
            createIfNew(
                existingKeys,
                created,
                RecommendationType.OPTIMISATION_ACHATS,
                RecommendationModuleSource.ACHAT,
                prediction.predictedDaysToStockout() <= 3 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING,
                "Risque de rupture sous " + prediction.predictedDaysToStockout() + " jour(s).",
                "Commander rapidement ce produit.",
                "Évite une rupture estimée à " + prediction.predictedDaysToStockout() + " jour(s).",
                agency,
                product,
                null
            );
        }
    }

    private void generateFromCriticalStocks(Set<String> existingKeys, List<Recommendation> created) {
        long threshold = regleMetierService.getDecimal(RegleMetierService.STOCK_CRITIQUE, BigDecimal.TEN).longValue();
        for (StockLevel stock : stockLevels()) {
            if (stock.quantity() > threshold) {
                continue;
            }
            createIfNew(
                existingKeys,
                created,
                RecommendationType.OPTIMISATION_ACHATS,
                RecommendationModuleSource.ACHAT,
                AlertSeverity.WARNING,
                "Stock critique : " + stock.quantity() + " unite(s) disponibles.",
                "Planifier un reapprovisionnement prioritaire.",
                "Réduction du risque de rupture sur produit critique.",
                stock.agency(),
                stock.product(),
                null
            );
        }
    }

    private void generateFromSalesAnomalies(Set<String> existingKeys, List<Recommendation> created) {
        AiDtos.SalesAnomalyAnalysisResponse analysis = aiAnalysisService.analyzeSalesAnomalies();
        for (AiDtos.SalesAnomalyResult anomaly : analysis.results()) {
            if (!anomaly.anomaly()) {
                continue;
            }
            Product product = resolveProduct(anomaly.productCode()).orElse(null);
            Agency agency = resolveAgency(anomaly.agencyCode()).orElse(null);
            boolean highSales = anomaly.zScore() > 0;
            createIfNew(
                existingKeys,
                created,
                RecommendationType.ALERTE_INTELLIGENTE,
                RecommendationModuleSource.IA,
                toSeverity(anomaly.alertLevel()),
                highSales ? "Ventes anormalement elevees detectees." : "Ventes anormalement basses detectees.",
                highSales ? "Prevoir un reapprovisionnement." : "Verifier la disponibilite du produit ou une erreur de saisie.",
                highSales ? "Anticipation du besoin stock." : "Réduction du risque d'erreur de saisie ou d'indisponibilité.",
                agency,
                product,
                null
            );
        }
    }

    private void generateFromDormantStocks(Set<String> existingKeys, List<Recommendation> created) {
        LocalDate minDate = LocalDate.now().minusDays(30);
        List<Sale> recentSales = saleRepository.findBySaleDateBetween(minDate, LocalDate.now());
        long threshold = regleMetierService.getDecimal(RegleMetierService.STOCK_CRITIQUE, BigDecimal.TEN).longValue();
        for (StockLevel stock : stockLevels()) {
            if (stock.quantity() <= threshold || hasRecentSale(recentSales, stock.product(), stock.agency())) {
                continue;
            }
            createIfNew(
                existingKeys,
                created,
                RecommendationType.OPTIMISATION_ACHATS,
                RecommendationModuleSource.ACHAT,
                AlertSeverity.INFO,
                "Stock disponible sans vente recente sur 30 jours.",
                "Analyser la rotation du produit ou lancer une action commerciale.",
                "Amélioration potentielle de la rotation du stock.",
                stock.agency(),
                stock.product(),
                null
            );
        }
    }

    private void createIfNew(
        Set<String> existingKeys,
        List<Recommendation> created,
        RecommendationType type,
        RecommendationModuleSource moduleSource,
        AlertSeverity severity,
        String message,
        String suggestedAction,
        String estimatedImpact,
        Agency agency,
        Product product,
        Alert relatedAlert
    ) {
        String key = key(type, agency, product, relatedAlert, message);
        if (existingKeys.contains(key)) {
            return;
        }

        Recommendation recommendation = new Recommendation();
        recommendation.setType(type);
        recommendation.setModuleSource(moduleSource);
        recommendation.setSeverity(severity);
        recommendation.setPriority(toPriority(severity));
        recommendation.setMessage(message);
        recommendation.setDescription(message + " Action proposée : " + suggestedAction);
        recommendation.setSuggestedAction(suggestedAction);
        recommendation.setEstimatedImpact(estimatedImpact);
        recommendation.setAgency(agency);
        recommendation.setProduct(product);
        recommendation.setRelatedAlert(relatedAlert);
        Recommendation saved = recommendationRepository.save(recommendation);
        existingKeys.add(key);
        created.add(saved);
    }

    private Set<String> existingKeys() {
        Set<String> keys = new HashSet<>();
        for (Recommendation recommendation : recommendationRepository.findAll()) {
            keys.add(key(
                recommendation.getType(),
                recommendation.getAgency(),
                recommendation.getProduct(),
                recommendation.getRelatedAlert(),
                recommendation.getMessage()
            ));
        }
        return keys;
    }

    private String key(RecommendationType type, Agency agency, Product product, Alert alert, String message) {
        return type + "|"
            + (agency == null ? "-" : agency.getId()) + "|"
            + (product == null ? "-" : product.getId()) + "|"
            + (alert == null ? "-" : alert.getId()) + "|"
            + message;
    }

    private String actionFromAlert(String message) {
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("rupture")) {
            return "Commander rapidement ce produit.";
        }
        if (lowerMessage.contains("quantite") || lowerMessage.contains("zscore")) {
            return "Verifier l'anomalie et ajuster le reapprovisionnement si necessaire.";
        }
        return "Analyser l'alerte et affecter une action au responsable metier.";
    }

    private ProductAgency extractProductAgency(String message) {
        Matcher matcher = PRODUCT_AGENCY_PATTERN.matcher(message);
        if (!matcher.find()) {
            return new ProductAgency(Optional.empty(), Optional.empty());
        }
        return new ProductAgency(resolveProduct(matcher.group(1)), resolveAgency(matcher.group(2)));
    }

    private Optional<Product> resolveProduct(String productCode) {
        try {
            return Optional.of(productService.getBySku(productCode));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Agency> resolveAgency(String agencyCode) {
        try {
            return Optional.of(agencyService.getByCode(agencyCode));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private List<StockLevel> stockLevels() {
        return stockMovementRepository.stockLevelsByEntity().stream()
            .map(row -> new StockLevel((Product) row[0], (Agency) row[1], toLong(row[2])))
            .sorted(Comparator.comparing(stock -> stock.product().getName()))
            .toList();
    }

    private boolean hasRecentSale(List<Sale> recentSales, Product product, Agency agency) {
        return recentSales.stream()
            .anyMatch(sale -> sale.getProduct().getId().equals(product.getId()) && sale.getAgency().getId().equals(agency.getId()));
    }

    private AlertSeverity toSeverity(String alertLevel) {
        if ("critique".equalsIgnoreCase(alertLevel) || "critical".equalsIgnoreCase(alertLevel)) {
            return AlertSeverity.CRITICAL;
        }
        if ("moyen".equalsIgnoreCase(alertLevel) || "medium".equalsIgnoreCase(alertLevel)) {
            return AlertSeverity.WARNING;
        }
        return AlertSeverity.INFO;
    }

    private Long toLong(Object value) {
        if (value instanceof Long typed) {
            return typed;
        }
        if (value instanceof Integer typed) {
            return typed.longValue();
        }
        if (value instanceof BigInteger typed) {
            return typed.longValue();
        }
        if (value instanceof BigDecimal typed) {
            return typed.longValue();
        }
        if (value instanceof Number typed) {
            return typed.longValue();
        }
        return 0L;
    }

    private void ensureSynthesisRecommendations() {
        Set<RecommendationType> existingTypes = recommendationRepository.findAll().stream()
            .map(Recommendation::getType)
            .collect(java.util.stream.Collectors.toSet());
        createMockRecommendationIfMissing(existingTypes,
            RecommendationType.OPTIMISATION_PLANNING,
            RecommendationModuleSource.PLANNING,
            RecommendationPriority.HAUTE,
            AlertSeverity.WARNING,
            "Réorganiser les ordres OP-2026-014 et OP-2026-018.",
            "La charge atelier dépasse la capacité disponible sur le créneau du matin.",
            "Décaler OP-2026-018 sur le créneau après-midi.",
            "Réduction estimée de 1 jour de retard."
        );
        createMockRecommendationIfMissing(existingTypes,
            RecommendationType.SIMULATION_WHAT_IF,
            RecommendationModuleSource.SIMULATION,
            RecommendationPriority.MOYENNE,
            AlertSeverity.INFO,
            "Scénario +15% demande : renforcer le stock tampon.",
            "La simulation montre un risque de tension sur les composants critiques.",
            "Augmenter temporairement le seuil de réapprovisionnement.",
            "Réduction du risque de rupture sur 7 jours."
        );
        createMockRecommendationIfMissing(existingTypes,
            RecommendationType.PREDICTION_NON_CONFORMITE,
            RecommendationModuleSource.QUALITE,
            RecommendationPriority.CRITIQUE,
            AlertSeverity.CRITICAL,
            "Contrôler la ligne L2 avant le prochain lot.",
            "Le taux prédit de non-conformité dépasse le seuil acceptable.",
            "Planifier un contrôle qualité renforcé.",
            "Baisse attendue des rebuts de 8%."
        );
        createMockRecommendationIfMissing(existingTypes,
            RecommendationType.OPTIMISATION_ACHATS,
            RecommendationModuleSource.ACHAT,
            RecommendationPriority.HAUTE,
            AlertSeverity.WARNING,
            "Commander CMP-044 auprès du fournisseur prioritaire.",
            "Le stock critique et le délai fournisseur créent un risque de rupture.",
            "Déclencher une commande urgente.",
            "Évite une rupture estimée sous 5 jours."
        );
        createMockRecommendationIfMissing(existingTypes,
            RecommendationType.ALERTE_INTELLIGENTE,
            RecommendationModuleSource.ALERTES,
            RecommendationPriority.MOYENNE,
            AlertSeverity.WARNING,
            "Traiter les alertes stock critiques non résolues.",
            "Plusieurs alertes actives concernent les mêmes références.",
            "Affecter un responsable et suivre la résolution.",
            "Amélioration du temps de réaction opérationnel."
        );
    }

    private void createMockRecommendationIfMissing(
        Set<RecommendationType> existingTypes,
        RecommendationType type,
        RecommendationModuleSource moduleSource,
        RecommendationPriority priority,
        AlertSeverity severity,
        String message,
        String description,
        String suggestedAction,
        String estimatedImpact
    ) {
        if (existingTypes.contains(type)) {
            return;
        }
        Recommendation recommendation = new Recommendation();
        recommendation.setType(type);
        recommendation.setModuleSource(moduleSource);
        recommendation.setPriority(priority);
        recommendation.setSeverity(severity);
        recommendation.setMessage(message);
        recommendation.setDescription(description);
        recommendation.setSuggestedAction(suggestedAction);
        recommendation.setEstimatedImpact(estimatedImpact);
        recommendation.setStatus(RecommendationStatus.PROPOSEE);
        recommendationRepository.save(recommendation);
        existingTypes.add(type);
    }

    private RecommendationPriority toPriority(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> RecommendationPriority.CRITIQUE;
            case WARNING -> RecommendationPriority.HAUTE;
            case INFO -> RecommendationPriority.MOYENNE;
        };
    }

    private int priorityRank(Recommendation recommendation) {
        RecommendationPriority priority = recommendation.getPriority() == null ? toPriority(recommendation.getSeverity()) : recommendation.getPriority();
        return switch (priority) {
            case CRITIQUE -> 4;
            case HAUTE -> 3;
            case MOYENNE -> 2;
            case FAIBLE -> 1;
        };
    }

    private HistoriqueModule toHistoriqueModule(RecommendationModuleSource moduleSource) {
        if (moduleSource == null) {
            return HistoriqueModule.DASHBOARD;
        }
        return switch (moduleSource) {
            case PLANNING -> HistoriqueModule.PRODUCTION;
            case SIMULATION -> HistoriqueModule.SIMULATION;
            case QUALITE -> HistoriqueModule.QUALITE;
            case ACHAT, STOCK -> HistoriqueModule.ACHAT;
            case ALERTES, IA -> HistoriqueModule.DASHBOARD;
        };
    }

    private Recommendation getEntity(Long id) {
        return recommendationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found: " + id));
    }

    private record ProductAgency(Optional<Product> product, Optional<Agency> agency) {
    }

    private record StockLevel(Product product, Agency agency, Long quantity) {
    }
}
