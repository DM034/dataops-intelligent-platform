package com.example.dataops.service;

import com.example.dataops.dto.AlerteDtos;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.model.AlertSeverity;
import com.example.dataops.model.Alerte;
import com.example.dataops.model.AlerteSourceModule;
import com.example.dataops.model.AlerteStatut;
import com.example.dataops.model.AlerteType;
import com.example.dataops.model.HistoriqueModule;
import com.example.dataops.model.JournalNiveau;
import com.example.dataops.model.NotificationNiveau;
import com.example.dataops.model.NotificationType;
import com.example.dataops.repository.AlerteRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlerteService {
    private final AlerteRepository repository;
    private final StockService stockService;
    private final HistoriqueActionService historiqueActionService;
    private final RegleMetierService regleMetierService;
    private final JournalActiviteService journalActiviteService;
    private final NotificationService notificationService;

    public AlerteService(AlerteRepository repository, StockService stockService, HistoriqueActionService historiqueActionService, RegleMetierService regleMetierService, JournalActiviteService journalActiviteService, NotificationService notificationService) {
        this.repository = repository;
        this.stockService = stockService;
        this.historiqueActionService = historiqueActionService;
        this.regleMetierService = regleMetierService;
        this.journalActiviteService = journalActiviteService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public AlerteDtos.AlerteListResponse findAll() {
        List<AlerteDtos.AlerteResponse> alertes = repository.findAllByOrderByDateCreationDesc().stream()
            .map(this::toResponse)
            .toList();
        return new AlerteDtos.AlerteListResponse(summary(alertes), alertes);
    }

    @Transactional(readOnly = true)
    public AlerteDtos.AlerteListResponse findActive() {
        List<AlerteDtos.AlerteResponse> alertes = repository.findByStatutOrderByDateCreationDesc(AlerteStatut.ACTIVE).stream()
            .map(this::toResponse)
            .toList();
        return new AlerteDtos.AlerteListResponse(summary(alertes), alertes);
    }

    @Transactional(readOnly = true)
    public AlerteDtos.AlerteResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public AlerteDtos.AlerteResponse resolve(Long id, HttpServletRequest request) {
        Alerte alerte = getEntity(id);
        AlerteStatut ancienneValeur = alerte.getStatut();
        alerte.setStatut(AlerteStatut.RESOLUE);
        alerte.setDateResolution(Instant.now());
        historiqueActionService.enregistrerAction(
            "RESOLUTION_ALERTE",
            toHistoriqueModule(alerte.getSourceModule()),
            "Resolution de l'alerte " + alerte.getType() + " : " + alerte.getMessage(),
            ancienneValeur.name(),
            AlerteStatut.RESOLUE.name(),
            alerte.getReferenceObjet(),
            request
        );
        return toResponse(alerte);
    }

    @Transactional
    public AlerteDtos.AlerteResponse ignore(Long id, HttpServletRequest request) {
        Alerte alerte = getEntity(id);
        AlerteStatut ancienneValeur = alerte.getStatut();
        alerte.setStatut(AlerteStatut.IGNOREE);
        alerte.setDateResolution(Instant.now());
        historiqueActionService.enregistrerAction(
            "IGNORER_ALERTE",
            toHistoriqueModule(alerte.getSourceModule()),
            "Alerte ignoree " + alerte.getType() + " : " + alerte.getMessage(),
            ancienneValeur.name(),
            AlerteStatut.IGNOREE.name(),
            alerte.getReferenceObjet(),
            request
        );
        return toResponse(alerte);
    }

    @Transactional
    public AlerteDtos.AlerteGenerateResponse generate() {
        List<Alerte> created = new ArrayList<>();
        BigDecimal nonConformityMax = regleMetierService.getDecimal(RegleMetierService.NON_CONFORMITE_MAX, BigDecimal.valueOf(5));
        BigDecimal capacityMax = regleMetierService.getDecimal(RegleMetierService.CAPACITE_MAX_ATELIER, BigDecimal.valueOf(120));
        BigDecimal purchaseAlertThreshold = regleMetierService.getDecimal(RegleMetierService.ALERTE_ACHAT_SEUIL, BigDecimal.valueOf(7));

        generateStockAlerts(created);
        createIfMissing(created, AlerteType.RETARD_PRODUCTION, AlertSeverity.WARNING, AlerteSourceModule.PRODUCTION,
            "Ordre OP-2026-014 probablement en retard : charge restante superieure au temps disponible.",
            "OP-2026-014");
        createIfMissing(created, AlerteType.SURCHARGE_RESSOURCE, AlertSeverity.WARNING, AlerteSourceModule.PRODUCTION,
            "Atelier Assemblage proche de la capacite maximale parametree (" + capacityMax + " ordres/jour).",
            "ATELIER-ASSEMBLAGE");
        createIfMissing(created, AlerteType.NON_CONFORMITE_ELEVEE, AlertSeverity.CRITICAL, AlerteSourceModule.QUALITE,
            "Taux de non-conformite ligne L2 au-dessus du seuil parametre de " + nonConformityMax + "%.",
            "LIGNE-L2");
        createIfMissing(created, AlerteType.ACHAT_URGENT, AlertSeverity.CRITICAL, AlerteSourceModule.ACHAT,
            "Achat urgent a prevoir pour composant critique CMP-044 sous le seuil de " + purchaseAlertThreshold + " jours.",
            "CMP-044");
        createIfMissing(created, AlerteType.FOURNISSEUR_MOINS_PERFORMANT, AlertSeverity.INFO, AlerteSourceModule.ACHAT,
            "Fournisseur FRS-DELTA : taux de livraison a l'heure inferieur a la moyenne du panel.",
            "FRS-DELTA");

        journalActiviteService.journaliser(JournalNiveau.INFO, "GENERATION_ALERTE", "ALERTES", "Generation automatique d'alertes", "createdCount=" + created.size(), "ALERTES");
        created.stream()
            .filter(alerte -> alerte.getNiveauCriticite() == AlertSeverity.CRITICAL)
            .findFirst()
            .ifPresent(alerte -> notificationService.create(
                null,
                alerte.getType() == AlerteType.ACHAT_URGENT ? "Achat urgent recommandé" : "Nouvelle alerte critique",
                alerte.getMessage(),
                alerte.getType() == AlerteType.ACHAT_URGENT ? NotificationType.ACHAT_URGENT : NotificationType.ALERTE_CRITIQUE,
                NotificationNiveau.CRITICAL,
                "alertes"
            ));
        return new AlerteDtos.AlerteGenerateResponse(
            created.size(),
            repository.findAllByOrderByDateCreationDesc().stream().map(this::toResponse).toList()
        );
    }

    private void generateStockAlerts(List<Alerte> created) {
        long threshold = regleMetierService.getDecimal(RegleMetierService.STOCK_CRITIQUE, BigDecimal.TEN).longValue();
        long criticalLimit = Math.max(1, threshold / 3);
        stockService.stockLevels().stream()
            .filter(stock -> stock.quantity() <= threshold)
            .forEach(stock -> createIfMissing(
                created,
                AlerteType.STOCK_CRITIQUE,
                stock.quantity() <= criticalLimit ? AlertSeverity.CRITICAL : AlertSeverity.WARNING,
                AlerteSourceModule.STOCK,
                "Stock critique pour " + stock.productName() + " / " + stock.agencyName() + " : " + stock.quantity() + " unite(s).",
                stock.productName() + "|" + stock.agencyName()
            ));
    }

    private void createIfMissing(List<Alerte> created, AlerteType type, AlertSeverity severity, AlerteSourceModule module, String message, String referenceObjet) {
        if (repository.findByTypeAndReferenceObjetAndStatut(type, referenceObjet, AlerteStatut.ACTIVE).isPresent()) {
            return;
        }

        Alerte alerte = new Alerte();
        alerte.setType(type);
        alerte.setNiveauCriticite(severity);
        alerte.setSourceModule(module);
        alerte.setMessage(message);
        alerte.setReferenceObjet(referenceObjet);
        created.add(repository.save(alerte));
    }

    private AlerteDtos.AlerteSummary summary(List<AlerteDtos.AlerteResponse> alertes) {
        return new AlerteDtos.AlerteSummary(
            alertes.stream().filter(alerte -> alerte.niveauCriticite() == AlertSeverity.CRITICAL).count(),
            alertes.stream().filter(alerte -> alerte.niveauCriticite() == AlertSeverity.WARNING).count(),
            alertes.stream().filter(alerte -> alerte.statut() == AlerteStatut.ACTIVE).count()
        );
    }

    private AlerteDtos.AlerteResponse toResponse(Alerte alerte) {
        return new AlerteDtos.AlerteResponse(
            alerte.getId(),
            alerte.getType(),
            alerte.getNiveauCriticite(),
            alerte.getMessage(),
            alerte.getSourceModule(),
            alerte.getStatut(),
            alerte.getDateCreation(),
            alerte.getDateResolution(),
            alerte.getReferenceObjet()
        );
    }

    private Alerte getEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Alerte not found: " + id));
    }

    private HistoriqueModule toHistoriqueModule(AlerteSourceModule module) {
        return switch (module) {
            case STOCK -> HistoriqueModule.STOCK;
            case PRODUCTION -> HistoriqueModule.PRODUCTION;
            case QUALITE -> HistoriqueModule.QUALITE;
            case ACHAT -> HistoriqueModule.ACHAT;
        };
    }
}
