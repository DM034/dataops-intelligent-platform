package com.example.dataops.config;

import com.example.dataops.model.Agency;
import com.example.dataops.model.Alerte;
import com.example.dataops.model.AlerteSourceModule;
import com.example.dataops.model.AlerteStatut;
import com.example.dataops.model.AlerteType;
import com.example.dataops.model.Alert;
import com.example.dataops.model.AlertSeverity;
import com.example.dataops.model.AppUser;
import com.example.dataops.model.DataLineage;
import com.example.dataops.model.DataQualityReport;
import com.example.dataops.model.HistoriqueAction;
import com.example.dataops.model.HistoriqueModule;
import com.example.dataops.model.ImportAudit;
import com.example.dataops.model.JournalActivite;
import com.example.dataops.model.JournalNiveau;
import com.example.dataops.model.Notification;
import com.example.dataops.model.NotificationNiveau;
import com.example.dataops.model.NotificationType;
import com.example.dataops.model.Product;
import com.example.dataops.model.Recommendation;
import com.example.dataops.model.RecommendationModuleSource;
import com.example.dataops.model.RecommendationPriority;
import com.example.dataops.model.RecommendationStatus;
import com.example.dataops.model.RecommendationType;
import com.example.dataops.model.Sale;
import com.example.dataops.model.StockMovement;
import com.example.dataops.model.StockMovementType;
import com.example.dataops.model.UserRole;
import com.example.dataops.repository.AgencyRepository;
import com.example.dataops.repository.AlerteRepository;
import com.example.dataops.repository.AlertRepository;
import com.example.dataops.repository.AppUserRepository;
import com.example.dataops.repository.DataLineageRepository;
import com.example.dataops.repository.DataQualityReportRepository;
import com.example.dataops.repository.HistoriqueActionRepository;
import com.example.dataops.repository.ImportAuditRepository;
import com.example.dataops.repository.JournalActiviteRepository;
import com.example.dataops.repository.NotificationRepository;
import com.example.dataops.repository.ProductRepository;
import com.example.dataops.repository.RecommendationRepository;
import com.example.dataops.repository.SaleRepository;
import com.example.dataops.repository.StockMovementRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DemoDataInitializer implements CommandLineRunner {
    private final AgencyRepository agencyRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AlertRepository alertRepository;
    private final AlerteRepository alerteRepository;
    private final RecommendationRepository recommendationRepository;
    private final DataQualityReportRepository dataQualityReportRepository;
    private final DataLineageRepository dataLineageRepository;
    private final ImportAuditRepository importAuditRepository;
    private final HistoriqueActionRepository historiqueActionRepository;
    private final JournalActiviteRepository journalActiviteRepository;
    private final NotificationRepository notificationRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(
        AgencyRepository agencyRepository,
        ProductRepository productRepository,
        SaleRepository saleRepository,
        StockMovementRepository stockMovementRepository,
        AlertRepository alertRepository,
        AlerteRepository alerteRepository,
        RecommendationRepository recommendationRepository,
        DataQualityReportRepository dataQualityReportRepository,
        DataLineageRepository dataLineageRepository,
        ImportAuditRepository importAuditRepository,
        HistoriqueActionRepository historiqueActionRepository,
        JournalActiviteRepository journalActiviteRepository,
        NotificationRepository notificationRepository,
        AppUserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.agencyRepository = agencyRepository;
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.alertRepository = alertRepository;
        this.alerteRepository = alerteRepository;
        this.recommendationRepository = recommendationRepository;
        this.dataQualityReportRepository = dataQualityReportRepository;
        this.dataLineageRepository = dataLineageRepository;
        this.importAuditRepository = importAuditRepository;
        this.historiqueActionRepository = historiqueActionRepository;
        this.journalActiviteRepository = journalActiviteRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureAdminUser();
        Map<String, Agency> agencies = ensureAgencies();
        Map<String, Product> products = ensureProducts();
        seedSales(agencies, products);
        seedStocks(agencies, products);
        seedAlerts();
        seedRecommendations(agencies, products);
        seedGovernance();
        seedTraceability();
        seedNotifications();
    }

    private void ensureAdminUser() {
        userRepository.findByUsername("admin").orElseGet(() -> {
            AppUser user = new AppUser();
            user.setUsername("admin");
            user.setEmail("admin@dataops.mg");
            user.setFullName("Administrateur DataOps Madagascar");
            user.setPasswordHash(passwordEncoder.encode("Admin1234"));
            user.setRole(UserRole.ADMIN);
            user.setActive(true);
            return userRepository.save(user);
        });
    }

    private Map<String, Agency> ensureAgencies() {
        Map<String, Agency> agencies = new LinkedHashMap<>();
        addAgency(agencies, "TANA", "Agence Antananarivo Analakely", "Antananarivo");
        addAgency(agencies, "TMM", "Agence Toamasina Port", "Toamasina");
        addAgency(agencies, "DIE", "Agence Antsiranana", "Antsiranana");
        addAgency(agencies, "MJN", "Agence Mahajanga", "Mahajanga");
        addAgency(agencies, "TOL", "Agence Toliara", "Toliara");
        addAgency(agencies, "FNR", "Agence Fianarantsoa", "Fianarantsoa");
        return agencies;
    }

    private void addAgency(Map<String, Agency> agencies, String code, String name, String city) {
        Agency agency = agencyRepository.findByCode(code).orElseGet(() -> {
            Agency created = new Agency();
            created.setCode(code);
            created.setName(name);
            created.setCity(city);
            created.setActive(true);
            return agencyRepository.save(created);
        });
        agencies.put(code, agency);
    }

    private Map<String, Product> ensureProducts() {
        Map<String, Product> products = new LinkedHashMap<>();
        addProduct(products, "RIZ-MAKALI", "Riz Makalioka 25kg", "Cereales", "95000");
        addProduct(products, "VAN-BIO", "Vanille bourbon 1kg", "Epicerie fine", "820000");
        addProduct(products, "CAFE-ROB", "Cafe robusta Itasy 1kg", "Boissons", "42000");
        addProduct(products, "GIROFLE", "Girofle de Sainte-Marie 1kg", "Epices", "68000");
        addProduct(products, "HUILE-COCO", "Huile de coco artisanale 1L", "Cosmetique", "28000");
        addProduct(products, "SAVON-ALA", "Savon ravintsara 100g", "Cosmetique", "6500");
        addProduct(products, "MIEL-MADA", "Miel eucalyptus 500g", "Epicerie", "18000");
        addProduct(products, "LITCHI", "Litchi seche 250g", "Fruits transformes", "22000");
        return products;
    }

    private void addProduct(Map<String, Product> products, String sku, String name, String category, String unitPrice) {
        Product product = productRepository.findBySku(sku).orElseGet(() -> {
            Product created = new Product();
            created.setSku(sku);
            created.setName(name);
            created.setCategory(category);
            created.setUnitPrice(new BigDecimal(unitPrice));
            created.setActive(true);
            return productRepository.save(created);
        });
        products.put(sku, product);
    }

    private void seedSales(Map<String, Agency> agencies, Map<String, Product> products) {
        if (saleRepository.count() > 0) {
            return;
        }
        List<String> agencyCodes = List.of("TANA", "TMM", "DIE", "MJN", "TOL", "FNR");
        List<String> productCodes = List.of("RIZ-MAKALI", "VAN-BIO", "CAFE-ROB", "GIROFLE", "HUILE-COCO", "SAVON-ALA", "MIEL-MADA", "LITCHI");
        LocalDate start = LocalDate.of(2026, 6, 1);
        int reference = 1001;
        for (int day = 0; day < 42; day++) {
            for (int agencyIndex = 0; agencyIndex < agencyCodes.size(); agencyIndex++) {
                for (int productIndex = 0; productIndex < productCodes.size(); productIndex++) {
                    if ((day + agencyIndex + productIndex) % 3 == 0) {
                        Product product = products.get(productCodes.get(productIndex));
                        int quantity = 3 + ((day * 2 + agencyIndex * 5 + productIndex * 7) % 28);
                        if ("VAN-BIO".equals(product.getSku()) && day == 21 && agencyIndex == 0) {
                            quantity = 95;
                        }
                        if ("RIZ-MAKALI".equals(product.getSku()) && day == 32 && agencyIndex == 1) {
                            quantity = 2;
                        }
                        Sale sale = new Sale();
                        sale.setAgency(agencies.get(agencyCodes.get(agencyIndex)));
                        sale.setProduct(product);
                        sale.setQuantity(quantity);
                        sale.setUnitPrice(product.getUnitPrice());
                        sale.setTotalAmount(product.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
                        sale.setSaleDate(start.plusDays(day));
                        sale.setReference("MDG-VTE-" + reference++);
                        saleRepository.save(sale);
                    }
                }
            }
        }
    }

    private void seedStocks(Map<String, Agency> agencies, Map<String, Product> products) {
        if (stockMovementRepository.count() > 0) {
            return;
        }
        int reference = 1;
        for (Agency agency : agencies.values()) {
            for (Product product : products.values()) {
                int base = switch (product.getSku()) {
                    case "VAN-BIO" -> 18;
                    case "GIROFLE" -> 35;
                    case "RIZ-MAKALI" -> 140;
                    case "SAVON-ALA" -> 260;
                    default -> 80;
                };
                saveStock(agency, product, StockMovementType.IN, base, "Stock initial depot Madagascar " + reference++);
                int sortie = Math.max(3, base / 4);
                saveStock(agency, product, StockMovementType.OUT, sortie, "Sortie ventes consolidees");
            }
        }
        saveStock(agencies.get("TMM"), products.get("VAN-BIO"), StockMovementType.OUT, 13, "Risque rupture export port Toamasina");
        saveStock(agencies.get("DIE"), products.get("GIROFLE"), StockMovementType.OUT, 29, "Pic demande girofle nord");
        saveStock(agencies.get("TOL"), products.get("HUILE-COCO"), StockMovementType.ADJUSTMENT, -18, "Correction inventaire Toliara");
    }

    private void saveStock(Agency agency, Product product, StockMovementType type, int quantity, String reason) {
        StockMovement movement = new StockMovement();
        movement.setAgency(agency);
        movement.setProduct(product);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setMovementDate(LocalDateTime.of(2026, 7, 1, 8, 0).plusHours(stockMovementRepository.count()));
        movement.setReason(reason);
        stockMovementRepository.save(movement);
    }

    private void seedAlerts() {
        if (alertRepository.count() == 0) {
            saveAlert(AlertSeverity.CRITICAL, "Stock critique vanille", "Le stock de vanille bourbon a Toamasina couvre moins de 5 jours.");
            saveAlert(AlertSeverity.WARNING, "Ventes basses riz", "Les ventes de riz Makalioka sont inferieures a la moyenne sur Antananarivo.");
            saveAlert(AlertSeverity.INFO, "Import CSV traite", "Le fichier ventes_juillet_madagascar.csv a ete controle et integre.");
        }
        if (alerteRepository.count() == 0) {
            saveAlerte(AlerteType.STOCK_CRITIQUE, AlertSeverity.CRITICAL, AlerteSourceModule.STOCK, "STOCK:TMM:VAN-BIO", "Rupture probable de vanille bourbon a Toamasina sous 4 jours.");
            saveAlerte(AlerteType.ACHAT_URGENT, AlertSeverity.CRITICAL, AlerteSourceModule.ACHAT, "ACHAT:VAN-BIO", "Achat urgent a planifier pour securiser les commandes export.");
            saveAlerte(AlerteType.NON_CONFORMITE_ELEVEE, AlertSeverity.WARNING, AlerteSourceModule.QUALITE, "QUALITE:HUILE-COCO:TOL", "Taux de non-conformite eleve sur huile de coco a Toliara.");
            saveAlerte(AlerteType.FOURNISSEUR_MOINS_PERFORMANT, AlertSeverity.WARNING, AlerteSourceModule.ACHAT, "FOURNISSEUR:F002", "Retards frequents du fournisseur girofle Sainte-Marie.");
        }
    }

    private void saveAlert(AlertSeverity severity, String title, String message) {
        Alert alert = new Alert();
        alert.setSeverity(severity);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setResolved(false);
        alertRepository.save(alert);
    }

    private void saveAlerte(AlerteType type, AlertSeverity severity, AlerteSourceModule module, String reference, String message) {
        Alerte alerte = new Alerte();
        alerte.setType(type);
        alerte.setNiveauCriticite(severity);
        alerte.setSourceModule(module);
        alerte.setReferenceObjet(reference);
        alerte.setMessage(message);
        alerte.setStatut(AlerteStatut.ACTIVE);
        alerteRepository.save(alerte);
    }

    private void seedRecommendations(Map<String, Agency> agencies, Map<String, Product> products) {
        boolean hasMadagascarRecommendations = recommendationRepository.findAll().stream()
            .anyMatch(recommendation -> recommendation.getAgency() != null && recommendation.getProduct() != null);
        if (hasMadagascarRecommendations) {
            return;
        }
        saveRecommendation(RecommendationType.OPTIMISATION_ACHATS, RecommendationModuleSource.ACHAT, RecommendationPriority.CRITIQUE, AlertSeverity.CRITICAL,
            "Commander rapidement de la vanille bourbon.",
            "Le stock de Toamasina est faible alors que les ventes export progressent.",
            "Lancer une commande fournisseur de 60 kg et prioriser le transport vers Toamasina.",
            "Impact fort: evite une rupture sur les ventes export.",
            agencies.get("TMM"), products.get("VAN-BIO"));
        saveRecommendation(RecommendationType.ALERTE_INTELLIGENTE, RecommendationModuleSource.IA, RecommendationPriority.HAUTE, AlertSeverity.WARNING,
            "Verifier les ventes basses du riz Makalioka.",
            "Le modele detecte une baisse inhabituelle a Antananarivo.",
            "Controler le stock rayon et verifier une eventuelle erreur de saisie.",
            "Impact moyen: fiabilise les KPI de ventes.",
            agencies.get("TANA"), products.get("RIZ-MAKALI"));
        saveRecommendation(RecommendationType.PREDICTION_NON_CONFORMITE, RecommendationModuleSource.QUALITE, RecommendationPriority.MOYENNE, AlertSeverity.WARNING,
            "Auditer le lot huile de coco Toliara.",
            "Les ajustements de stock et retours qualite augmentent sur ce produit.",
            "Isoler le lot, verifier le conditionnement et tracer la decision dans l'historique.",
            "Impact qualite: reduit les retours client.",
            agencies.get("TOL"), products.get("HUILE-COCO"));
    }

    private void saveRecommendation(
        RecommendationType type,
        RecommendationModuleSource module,
        RecommendationPriority priority,
        AlertSeverity severity,
        String message,
        String description,
        String action,
        String impact,
        Agency agency,
        Product product
    ) {
        Recommendation recommendation = new Recommendation();
        recommendation.setType(type);
        recommendation.setModuleSource(module);
        recommendation.setPriority(priority);
        recommendation.setSeverity(severity);
        recommendation.setMessage(message);
        recommendation.setDescription(description);
        recommendation.setSuggestedAction(action);
        recommendation.setEstimatedImpact(impact);
        recommendation.setStatus(RecommendationStatus.PROPOSEE);
        recommendation.setAgency(agency);
        recommendation.setProduct(product);
        recommendationRepository.save(recommendation);
    }

    private void seedGovernance() {
        if (dataQualityReportRepository.count() == 0) {
            saveQuality("ventes_juillet_madagascar.csv", "VENTES", 420, 397, 23, 8, "94.30", "92.80", "98.10", "91.40", "94.15");
            saveQuality("stocks_madagascar_q3.csv", "STOCKS", 96, 91, 5, 2, "96.90", "94.80", "97.90", "93.50", "95.78");
            saveQuality("produits_mada_catalogue.csv", "PRODUITS", 64, 62, 2, 0, "98.40", "96.90", "100.00", "95.30", "97.65");
        }
        if (dataLineageRepository.count() == 0) {
            saveLineage("ventes_juillet_madagascar.csv", "CSV", "Controle colonnes, formats dates et doublons", "Calcul CA et agregats KPI", "Table sales PostgreSQL", "Dashboard global et KPI ventes", "VALIDE");
            saveLineage("stocks_madagascar_q3.csv", "CSV", "Controle quantites et types IN/OUT", "Consolidation mouvements de stock", "Table stock_movements PostgreSQL", "Stock critique et alertes", "VALIDE_AVEC_ALERTES");
            saveLineage("qualite_lots_toliara.xlsx", "EXCEL", "Controle lots et non-conformites", "Normalisation par produit/agence", "Tables alertes et recommandations", "Dashboard gouvernance", "A_SURVEILLER");
        }
        if (importAuditRepository.count() == 0) {
            saveImportAudit("ventes_juillet_madagascar.csv", 420, 397, 23, "VALIDE_AVEC_ERREURS");
            saveImportAudit("stocks_madagascar_q3.csv", 96, 91, 5, "VALIDE_AVEC_ERREURS");
            saveImportAudit("produits_mada_catalogue.csv", 64, 62, 2, "VALIDE");
        }
    }

    private void saveQuality(String fileId, String source, int total, int valid, int errors, int duplicates, String completeness, String validity, String uniqueness, String consistency, String score) {
        DataQualityReport report = new DataQualityReport();
        report.setImportFileId(fileId);
        report.setSourceName(source);
        report.setTotalRows(total);
        report.setValidRows(valid);
        report.setErrorRows(errors);
        report.setDuplicateRecords(duplicates);
        report.setCompletenessRate(new BigDecimal(completeness));
        report.setValidityRate(new BigDecimal(validity));
        report.setUniquenessRate(new BigDecimal(uniqueness));
        report.setConsistencyRate(new BigDecimal(consistency));
        report.setGlobalScore(new BigDecimal(score));
        dataQualityReportRepository.save(report);
    }

    private void saveLineage(String source, String type, String validation, String transformation, String storage, String dashboard, String status) {
        Instant now = Instant.parse("2026-07-15T08:00:00Z");
        DataLineage lineage = new DataLineage();
        lineage.setSourceName(source);
        lineage.setSourceType(type);
        lineage.setImportDate(now);
        lineage.setValidationDate(now.plusSeconds(120));
        lineage.setTransformationDate(now.plusSeconds(300));
        lineage.setStorageDate(now.plusSeconds(420));
        lineage.setDashboardDate(now.plusSeconds(600));
        lineage.setValidationStep(validation);
        lineage.setTransformationStep(transformation);
        lineage.setStorageStep(storage);
        lineage.setDashboardStep(dashboard);
        lineage.setStatus(status);
        dataLineageRepository.save(lineage);
    }

    private void saveImportAudit(String fileName, int total, int success, int failed, String status) {
        ImportAudit audit = new ImportAudit();
        audit.setFileName(fileName);
        audit.setImportedBy("admin");
        audit.setTotalRows(total);
        audit.setSuccessRows(success);
        audit.setFailedRows(failed);
        audit.setStatus(status);
        importAuditRepository.save(audit);
    }

    private void seedTraceability() {
        if (historiqueActionRepository.count() == 0) {
            saveHistory("IMPORT_CSV", HistoriqueModule.STOCK, "Import du fichier stocks_madagascar_q3.csv", "Aucun", "91 lignes valides, 5 erreurs", "IMPORT-STOCK-MG");
            saveHistory("VALIDATION_RECOMMANDATION", HistoriqueModule.ACHAT, "Validation d'une recommandation d'achat vanille", "Statut PROPOSEE", "Statut VALIDEE", "REC-VAN-BIO");
            saveHistory("RESOLUTION_ALERTE", HistoriqueModule.STOCK, "Plan d'action lance pour rupture Toamasina", "Alerte ACTIVE", "Alerte RESOLUE", "STOCK:TMM:VAN-BIO");
        }
        if (journalActiviteRepository.count() == 0) {
            saveJournal(JournalNiveau.INFO, "IMPORT_CSV", "IMPORT", "Import ventes Madagascar execute", "397 lignes integrees, 23 lignes rejetees", "ventes_juillet_madagascar.csv");
            saveJournal(JournalNiveau.WARNING, "GENERATION_ALERTE", "STOCK", "Stock critique detecte sur vanille bourbon", "Agence=TMM, Produit=VAN-BIO", "STOCK:TMM:VAN-BIO");
            saveJournal(JournalNiveau.INFO, "APPEL_MODULE_PREDICTIF", "IA", "Analyse anomalies ventes executee", "Methode Z-score et moyenne mobile 7 jours", "AI-SALES-MG");
        }
    }

    private void saveHistory(String action, HistoriqueModule module, String description, String oldValue, String newValue, String reference) {
        HistoriqueAction history = new HistoriqueAction();
        history.setUtilisateurId("admin");
        history.setUtilisateurNom("Administrateur DataOps Madagascar");
        history.setAction(action);
        history.setModule(module);
        history.setDescription(description);
        history.setAncienneValeur(oldValue);
        history.setNouvelleValeur(newValue);
        history.setReferenceObjet(reference);
        history.setAdresseIp("127.0.0.1");
        historiqueActionRepository.save(history);
    }

    private void saveJournal(JournalNiveau level, String type, String module, String message, String details, String reference) {
        JournalActivite event = new JournalActivite();
        event.setNiveau(level);
        event.setTypeEvenement(type);
        event.setModule(module);
        event.setMessage(message);
        event.setUtilisateur("admin");
        event.setDetails(details);
        event.setReferenceObjet(reference);
        journalActiviteRepository.save(event);
    }

    private void seedNotifications() {
        if (notificationRepository.count() > 0) {
            return;
        }
        saveNotification("Alerte critique stock", "Vanille bourbon critique a Toamasina: commande urgente recommandee.", NotificationType.ALERTE_CRITIQUE, NotificationNiveau.CRITICAL, "/alertes");
        saveNotification("Achat urgent recommande", "Reapprovisionnement vanille bourbon propose pour proteger les ventes export.", NotificationType.ACHAT_URGENT, NotificationNiveau.WARNING, "/recommandations");
        saveNotification("Rapport gouvernance pret", "Le rapport qualite des donnees Madagascar est disponible.", NotificationType.RAPPORT_EXPORTE, NotificationNiveau.SUCCESS, "/data-governance");
    }

    private void saveNotification(String title, String message, NotificationType type, NotificationNiveau level, String link) {
        Notification notification = new Notification();
        notification.setUtilisateurId("admin");
        notification.setTitre(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setNiveau(level);
        notification.setLu(false);
        notification.setLienAction(link);
        notificationRepository.save(notification);
    }
}
