package com.example.dataops.service;

import com.example.dataops.dto.AlerteDtos;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.model.AlertSeverity;
import com.example.dataops.model.Alerte;
import com.example.dataops.model.AlerteSourceModule;
import com.example.dataops.model.AlerteStatut;
import com.example.dataops.model.AlerteType;
import com.example.dataops.repository.AlerteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlerteService {
    private static final long CRITICAL_STOCK_THRESHOLD = 10L;

    private final AlerteRepository repository;
    private final StockService stockService;

    public AlerteService(AlerteRepository repository, StockService stockService) {
        this.repository = repository;
        this.stockService = stockService;
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
    public AlerteDtos.AlerteResponse resolve(Long id) {
        Alerte alerte = getEntity(id);
        alerte.setStatut(AlerteStatut.RESOLUE);
        alerte.setDateResolution(Instant.now());
        return toResponse(alerte);
    }

    @Transactional
    public AlerteDtos.AlerteResponse ignore(Long id) {
        Alerte alerte = getEntity(id);
        alerte.setStatut(AlerteStatut.IGNOREE);
        alerte.setDateResolution(Instant.now());
        return toResponse(alerte);
    }

    @Transactional
    public AlerteDtos.AlerteGenerateResponse generate() {
        List<Alerte> created = new ArrayList<>();

        generateStockAlerts(created);
        createIfMissing(created, AlerteType.RETARD_PRODUCTION, AlertSeverity.WARNING, AlerteSourceModule.PRODUCTION,
            "Ordre OP-2026-014 probablement en retard : charge restante superieure au temps disponible.",
            "OP-2026-014");
        createIfMissing(created, AlerteType.SURCHARGE_RESSOURCE, AlertSeverity.WARNING, AlerteSourceModule.PRODUCTION,
            "Atelier Assemblage charge a 118% sur les prochaines 24h.",
            "ATELIER-ASSEMBLAGE");
        createIfMissing(created, AlerteType.NON_CONFORMITE_ELEVEE, AlertSeverity.CRITICAL, AlerteSourceModule.QUALITE,
            "Taux de non-conformite ligne L2 a 8.4%, au-dessus du seuil de 5%.",
            "LIGNE-L2");
        createIfMissing(created, AlerteType.ACHAT_URGENT, AlertSeverity.CRITICAL, AlerteSourceModule.ACHAT,
            "Achat urgent a prevoir pour composant critique CMP-044 sous 3 jours.",
            "CMP-044");
        createIfMissing(created, AlerteType.FOURNISSEUR_MOINS_PERFORMANT, AlertSeverity.INFO, AlerteSourceModule.ACHAT,
            "Fournisseur FRS-DELTA : taux de livraison a l'heure inferieur a la moyenne du panel.",
            "FRS-DELTA");

        return new AlerteDtos.AlerteGenerateResponse(
            created.size(),
            repository.findAllByOrderByDateCreationDesc().stream().map(this::toResponse).toList()
        );
    }

    private void generateStockAlerts(List<Alerte> created) {
        stockService.stockLevels().stream()
            .filter(stock -> stock.quantity() <= CRITICAL_STOCK_THRESHOLD)
            .forEach(stock -> createIfMissing(
                created,
                AlerteType.STOCK_CRITIQUE,
                stock.quantity() <= 3 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING,
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
}
