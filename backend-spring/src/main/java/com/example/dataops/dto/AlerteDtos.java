package com.example.dataops.dto;

import com.example.dataops.model.AlertSeverity;
import com.example.dataops.model.AlerteSourceModule;
import com.example.dataops.model.AlerteStatut;
import com.example.dataops.model.AlerteType;

import java.time.Instant;
import java.util.List;

public final class AlerteDtos {
    private AlerteDtos() {
    }

    public record AlerteResponse(
        Long id,
        AlerteType type,
        AlertSeverity niveauCriticite,
        String message,
        AlerteSourceModule sourceModule,
        AlerteStatut statut,
        Instant dateCreation,
        Instant dateResolution,
        String referenceObjet
    ) {
    }

    public record AlerteSummary(long criticalCount, long warningCount, long activeCount) {
    }

    public record AlerteListResponse(AlerteSummary summary, List<AlerteResponse> alertes) {
    }

    public record AlerteGenerateResponse(int createdCount, List<AlerteResponse> alertes) {
    }
}
