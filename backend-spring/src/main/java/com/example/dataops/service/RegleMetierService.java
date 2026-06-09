package com.example.dataops.service;

import com.example.dataops.dto.RegleMetierDtos;
import com.example.dataops.exception.BusinessException;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.model.RegleMetier;
import com.example.dataops.model.RegleMetierModule;
import com.example.dataops.model.TypeValeurRegle;
import com.example.dataops.repository.RegleMetierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RegleMetierService {
    public static final String STOCK_CRITIQUE = "STOCK_CRITIQUE_SEUIL";
    public static final String DELAI_MIN_FOURNISSEUR = "FOURNISSEUR_DELAI_MIN";
    public static final String NON_CONFORMITE_MAX = "QUALITE_NON_CONFORMITE_MAX";
    public static final String CAPACITE_MAX_ATELIER = "PRODUCTION_CAPACITE_MAX_ATELIER";
    public static final String RETARD_PRODUCTION_SEUIL = "PRODUCTION_RETARD_SEUIL";
    public static final String PRIORITE_CLIENT_COEFFICIENT = "SIMULATION_PRIORITE_CLIENT_COEF";
    public static final String COUT_RETARD_ESTIME = "SIMULATION_COUT_RETARD_ESTIME";
    public static final String ALERTE_ACHAT_SEUIL = "ACHAT_ALERTE_SEUIL";

    private final RegleMetierRepository repository;

    public RegleMetierService(RegleMetierRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<RegleMetierDtos.RegleMetierResponse> findAll(RegleMetierModule module) {
        ensureDefaultRules();
        List<RegleMetier> rules = module == null
            ? repository.findAllByOrderByModuleAscCodeAsc()
            : repository.findByModuleOrderByCode(module);
        return rules.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RegleMetierDtos.RegleMetierResponse findByCode(String code) {
        return toResponse(entityByCode(code));
    }

    @Transactional
    public RegleMetierDtos.RegleMetierResponse update(String code, RegleMetierDtos.UpdateRegleMetierRequest request) {
        RegleMetier rule = entityByCode(code);
        validateValue(rule.getTypeValeur(), request.valeur());
        rule.setValeur(request.valeur());
        if (request.actif() != null) {
            rule.setActif(request.actif());
        }
        return toResponse(rule);
    }

    @Transactional
    public RegleMetierDtos.RegleMetierResponse create(RegleMetierDtos.CreateRegleMetierRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new BusinessException("Business rule already exists: " + request.code());
        }
        validateValue(request.typeValeur(), request.valeur());
        RegleMetier rule = new RegleMetier();
        rule.setCode(request.code());
        rule.setLibelle(request.libelle());
        rule.setDescription(request.description());
        rule.setModule(request.module());
        rule.setValeur(request.valeur());
        rule.setUnite(request.unite());
        rule.setTypeValeur(request.typeValeur());
        rule.setActif(request.actif() == null || request.actif());
        return toResponse(repository.save(rule));
    }

    @Transactional(readOnly = true)
    public BigDecimal getDecimal(String code, BigDecimal fallback) {
        return repository.findByCode(code)
            .filter(RegleMetier::isActif)
            .map(RegleMetier::getValeur)
            .map(BigDecimal::new)
            .orElse(fallback);
    }

    @Transactional(readOnly = true)
    public boolean getBoolean(String code, boolean fallback) {
        return repository.findByCode(code)
            .filter(RegleMetier::isActif)
            .map(RegleMetier::getValeur)
            .map(Boolean::parseBoolean)
            .orElse(fallback);
    }

    @Transactional(readOnly = true)
    public String getText(String code, String fallback) {
        return repository.findByCode(code)
            .filter(RegleMetier::isActif)
            .map(RegleMetier::getValeur)
            .orElse(fallback);
    }

    @Transactional
    public void ensureDefaultRules() {
        createDefault(STOCK_CRITIQUE, "Seuil de stock critique", "Quantite minimale avant signalement d'un stock critique.", RegleMetierModule.STOCK, "10", "unites", TypeValeurRegle.NOMBRE);
        createDefault(DELAI_MIN_FOURNISSEUR, "Delai minimum fournisseur", "Delai minimum attendu pour une commande fournisseur.", RegleMetierModule.ACHAT, "5", "jours", TypeValeurRegle.NOMBRE);
        createDefault(NON_CONFORMITE_MAX, "Taux maximum de non-conformite", "Taux maximum acceptable avant alerte qualite.", RegleMetierModule.QUALITE, "5", "%", TypeValeurRegle.POURCENTAGE);
        createDefault(CAPACITE_MAX_ATELIER, "Capacite maximale atelier", "Charge maximale supportee par un atelier sur une periode.", RegleMetierModule.PRODUCTION, "120", "ordres/jour", TypeValeurRegle.NOMBRE);
        createDefault(RETARD_PRODUCTION_SEUIL, "Seuil de retard production", "Nombre de jours de retard avant alerte production.", RegleMetierModule.PRODUCTION, "2", "jours", TypeValeurRegle.NOMBRE);
        createDefault(PRIORITE_CLIENT_COEFFICIENT, "Coefficient de priorite client", "Coefficient applique dans les simulations de priorisation client.", RegleMetierModule.SIMULATION, "1.5", "coefficient", TypeValeurRegle.NOMBRE);
        createDefault(COUT_RETARD_ESTIME, "Cout estime d'un retard", "Cout metier estime par jour de retard.", RegleMetierModule.SIMULATION, "250000", "MGA/jour", TypeValeurRegle.NOMBRE);
        createDefault(ALERTE_ACHAT_SEUIL, "Seuil de declenchement alerte achat", "Nombre de jours de couverture stock sous lequel un achat est recommande.", RegleMetierModule.ACHAT, "7", "jours", TypeValeurRegle.NOMBRE);
    }

    private void createDefault(String code, String libelle, String description, RegleMetierModule module, String valeur, String unite, TypeValeurRegle typeValeur) {
        if (repository.existsByCode(code)) {
            return;
        }
        RegleMetier rule = new RegleMetier();
        rule.setCode(code);
        rule.setLibelle(libelle);
        rule.setDescription(description);
        rule.setModule(module);
        rule.setValeur(valeur);
        rule.setUnite(unite);
        rule.setTypeValeur(typeValeur);
        rule.setActif(true);
        repository.save(rule);
    }

    private RegleMetier entityByCode(String code) {
        return repository.findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Business rule not found: " + code));
    }

    private void validateValue(TypeValeurRegle type, String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Rule value is required");
        }
        try {
            if (type == TypeValeurRegle.NOMBRE || type == TypeValeurRegle.POURCENTAGE) {
                new BigDecimal(value);
            } else if (type == TypeValeurRegle.BOOLEEN && !List.of("true", "false").contains(value.toLowerCase())) {
                throw new BusinessException("Boolean rule value must be true or false");
            }
        } catch (NumberFormatException exception) {
            throw new BusinessException("Numeric rule value is invalid: " + value);
        }
    }

    private RegleMetierDtos.RegleMetierResponse toResponse(RegleMetier rule) {
        return new RegleMetierDtos.RegleMetierResponse(
            rule.getId(),
            rule.getCode(),
            rule.getLibelle(),
            rule.getDescription(),
            rule.getModule(),
            rule.getValeur(),
            rule.getUnite(),
            rule.getTypeValeur(),
            rule.isActif(),
            rule.getDateModification()
        );
    }
}
