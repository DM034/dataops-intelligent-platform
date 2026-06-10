package com.example.dataops.service;

import com.example.dataops.dto.AiDtos;
import com.example.dataops.dto.RecommendationDtos;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.Agency;
import com.example.dataops.model.Alert;
import com.example.dataops.model.AlertSeverity;
import com.example.dataops.model.NotificationNiveau;
import com.example.dataops.model.NotificationType;
import com.example.dataops.model.Product;
import com.example.dataops.model.Recommendation;
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
    private final NotificationService notificationService;

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
        NotificationService notificationService
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
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<RecommendationDtos.RecommendationResponse> findAll() {
        return recommendationRepository.findAllByOrderByCreatedAtDesc().stream()
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
        recommendation.setStatus(status);
        blockchainService.append("RECOMMENDATION_STATUS_UPDATED", "system", "recommendationId=" + id + "|status=" + status);
        if (status == RecommendationStatus.DONE || status == RecommendationStatus.IN_PROGRESS) {
            notificationService.create(null, "Décision validée", "Décision enregistrée pour la recommandation #" + id, NotificationType.DECISION_VALIDEE, NotificationNiveau.SUCCESS, "recommendations");
        }
        return mapper.toRecommendationResponse(recommendation);
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
            notificationService.create(null, "Achat urgent recommandé", created.size() + " recommandation(s) métier générée(s).", NotificationType.ACHAT_URGENT, NotificationNiveau.WARNING, "recommendations");
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
                RecommendationType.AI_ALERT,
                alert.getSeverity(),
                "Alerte IA a traiter : " + alert.getTitle(),
                actionFromAlert(alert.getMessage()),
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
                RecommendationType.STOCKOUT_RISK,
                prediction.predictedDaysToStockout() <= 3 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING,
                "Risque de rupture sous " + prediction.predictedDaysToStockout() + " jour(s).",
                "Commander rapidement ce produit.",
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
                RecommendationType.CRITICAL_STOCK,
                AlertSeverity.WARNING,
                "Stock critique : " + stock.quantity() + " unite(s) disponibles.",
                "Planifier un reapprovisionnement prioritaire.",
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
                RecommendationType.SALES_ANOMALY,
                toSeverity(anomaly.alertLevel()),
                highSales ? "Ventes anormalement elevees detectees." : "Ventes anormalement basses detectees.",
                highSales ? "Prevoir un reapprovisionnement." : "Verifier la disponibilite du produit ou une erreur de saisie.",
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
                RecommendationType.DORMANT_STOCK,
                AlertSeverity.INFO,
                "Stock disponible sans vente recente sur 30 jours.",
                "Analyser la rotation du produit ou lancer une action commerciale.",
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
        AlertSeverity severity,
        String message,
        String suggestedAction,
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
        recommendation.setSeverity(severity);
        recommendation.setMessage(message);
        recommendation.setSuggestedAction(suggestedAction);
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

    private Recommendation getEntity(Long id) {
        return recommendationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found: " + id));
    }

    private record ProductAgency(Optional<Product> product, Optional<Agency> agency) {
    }

    private record StockLevel(Product product, Agency agency, Long quantity) {
    }
}
