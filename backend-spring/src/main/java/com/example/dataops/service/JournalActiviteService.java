package com.example.dataops.service;

import com.example.dataops.dto.JournalActiviteDtos;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.model.JournalActivite;
import com.example.dataops.model.JournalNiveau;
import com.example.dataops.repository.JournalActiviteRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class JournalActiviteService {
    private final JournalActiviteRepository repository;

    public JournalActiviteService(JournalActiviteRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JournalActiviteDtos.JournalActiviteResponse journaliser(
        JournalNiveau niveau,
        String typeEvenement,
        String module,
        String message,
        String utilisateur,
        String details,
        String referenceObjet
    ) {
        JournalActivite event = new JournalActivite();
        event.setNiveau(niveau == null ? JournalNiveau.INFO : niveau);
        event.setTypeEvenement(typeEvenement);
        event.setModule(module == null || module.isBlank() ? "SYSTEME" : module);
        event.setMessage(message);
        event.setUtilisateur(utilisateur == null || utilisateur.isBlank() ? currentUser() : utilisateur);
        event.setDetails(details);
        event.setReferenceObjet(referenceObjet);
        return toResponse(repository.save(event));
    }

    public JournalActiviteDtos.JournalActiviteResponse journaliser(
        JournalNiveau niveau,
        String typeEvenement,
        String module,
        String message,
        String details,
        String referenceObjet
    ) {
        return journaliser(niveau, typeEvenement, module, message, currentUser(), details, referenceObjet);
    }

    @Transactional(readOnly = true)
    public JournalActiviteDtos.JournalActivitePageResponse search(
        JournalNiveau niveau,
        String module,
        Instant dateDebut,
        Instant dateFin,
        String utilisateur,
        int page,
        int size
    ) {
        Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), 100),
            Sort.by(Sort.Order.asc("niveau"), Sort.Order.desc("dateEvenement"))
        );
        Page<JournalActivite> result = repository.findAll(specification(niveau, module, dateDebut, dateFin, utilisateur), pageable);
        return new JournalActiviteDtos.JournalActivitePageResponse(
            result.getContent().stream().map(this::toResponse).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public JournalActiviteDtos.JournalActiviteResponse findById(Long id) {
        return toResponse(repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Journal activity not found: " + id)));
    }

    private Specification<JournalActivite> specification(JournalNiveau niveau, String module, Instant dateDebut, Instant dateFin, String utilisateur) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (niveau != null) {
                predicates.add(builder.equal(root.get("niveau"), niveau));
            }
            if (module != null && !module.isBlank()) {
                predicates.add(builder.equal(builder.lower(root.get("module")), module.toLowerCase()));
            }
            if (dateDebut != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("dateEvenement"), dateDebut));
            }
            if (dateFin != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("dateEvenement"), dateFin));
            }
            if (utilisateur != null && !utilisateur.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("utilisateur")), "%" + utilisateur.toLowerCase() + "%"));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private JournalActiviteDtos.JournalActiviteResponse toResponse(JournalActivite event) {
        return new JournalActiviteDtos.JournalActiviteResponse(
            event.getId(),
            event.getNiveau(),
            event.getTypeEvenement(),
            event.getModule(),
            event.getMessage(),
            event.getUtilisateur(),
            event.getDateEvenement(),
            event.getDetails(),
            event.getReferenceObjet()
        );
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || authentication.getName() == null ? "system" : authentication.getName();
    }
}
