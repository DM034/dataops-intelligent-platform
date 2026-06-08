package com.example.dataops.dto;

import com.example.dataops.model.AlertSeverity;
import com.example.dataops.model.RecommendationStatus;
import com.example.dataops.model.RecommendationType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class RecommendationDtos {
    private RecommendationDtos() {
    }

    public record RecommendationResponse(
        Long id,
        RecommendationType type,
        AlertSeverity severity,
        String message,
        String suggestedAction,
        Long agencyId,
        String agencyName,
        Long productId,
        String productName,
        Long relatedAlertId,
        Instant createdAt,
        RecommendationStatus status
    ) {
    }

    public record RecommendationStatusRequest(@NotNull RecommendationStatus status) {
    }

    public record RecommendationGenerateResponse(int createdCount, List<RecommendationResponse> recommendations) {
    }
}
