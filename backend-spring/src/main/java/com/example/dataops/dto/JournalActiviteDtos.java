package com.example.dataops.dto;

import com.example.dataops.model.JournalNiveau;

import java.time.Instant;
import java.util.List;

public final class JournalActiviteDtos {
    private JournalActiviteDtos() {
    }

    public record JournalActiviteResponse(
        Long id,
        JournalNiveau niveau,
        String typeEvenement,
        String module,
        String message,
        String utilisateur,
        Instant dateEvenement,
        String details,
        String referenceObjet
    ) {
    }

    public record JournalActivitePageResponse(
        List<JournalActiviteResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }
}
