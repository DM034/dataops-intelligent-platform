package com.example.dataops.service;

import com.example.dataops.dto.HistoriqueActionDtos;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.model.HistoriqueAction;
import com.example.dataops.model.HistoriqueModule;
import com.example.dataops.repository.HistoriqueActionRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class HistoriqueActionService {
    private final HistoriqueActionRepository repository;

    public HistoriqueActionService(HistoriqueActionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public HistoriqueActionDtos.HistoriqueActionResponse enregistrerAction(
        String action,
        HistoriqueModule module,
        String description,
        String ancienneValeur,
        String nouvelleValeur,
        String referenceObjet,
        HttpServletRequest request
    ) {
        HistoriqueAction historique = new HistoriqueAction();
        historique.setUtilisateurId(currentUserId());
        historique.setUtilisateurNom(currentUserName());
        historique.setAction(action);
        historique.setModule(module);
        historique.setDescription(description);
        historique.setAncienneValeur(ancienneValeur);
        historique.setNouvelleValeur(nouvelleValeur);
        historique.setReferenceObjet(referenceObjet);
        historique.setAdresseIp(resolveIp(request));
        return toResponse(repository.save(historique));
    }

    @Transactional(readOnly = true)
    public List<HistoriqueActionDtos.HistoriqueActionResponse> findAll() {
        return repository.findAllByOrderByDateActionDesc().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public HistoriqueActionDtos.HistoriqueActionResponse findById(Long id) {
        return toResponse(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Historique action not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<HistoriqueActionDtos.HistoriqueActionResponse> search(HistoriqueModule module, String utilisateur, Instant dateDebut, Instant dateFin, String action) {
        return repository.findAll(specification(module, utilisateur, dateDebut, dateFin, action)).stream()
            .sorted((left, right) -> right.getDateAction().compareTo(left.getDateAction()))
            .map(this::toResponse)
            .toList();
    }

    private Specification<HistoriqueAction> specification(HistoriqueModule module, String utilisateur, Instant dateDebut, Instant dateFin, String action) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (module != null) {
                predicates.add(builder.equal(root.get("module"), module));
            }
            if (utilisateur != null && !utilisateur.isBlank()) {
                String pattern = "%" + utilisateur.toLowerCase() + "%";
                predicates.add(builder.or(
                    builder.like(builder.lower(root.get("utilisateurNom")), pattern),
                    builder.like(builder.lower(root.get("utilisateurId")), pattern)
                ));
            }
            if (dateDebut != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("dateAction"), dateDebut));
            }
            if (dateFin != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("dateAction"), dateFin));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("action")), "%" + action.toLowerCase() + "%"));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private HistoriqueActionDtos.HistoriqueActionResponse toResponse(HistoriqueAction historique) {
        return new HistoriqueActionDtos.HistoriqueActionResponse(
            historique.getId(),
            historique.getUtilisateurId(),
            historique.getUtilisateurNom(),
            historique.getAction(),
            historique.getModule(),
            historique.getDescription(),
            historique.getAncienneValeur(),
            historique.getNouvelleValeur(),
            historique.getDateAction(),
            historique.getReferenceObjet(),
            historique.getAdresseIp()
        );
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || authentication.getName() == null ? "mock-user" : authentication.getName();
    }

    private String currentUserName() {
        return currentUserId();
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
