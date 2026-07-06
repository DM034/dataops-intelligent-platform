package com.example.dataops.dto;

import com.example.dataops.model.NotificationNiveau;
import com.example.dataops.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record NotificationResponse(
        Long id,
        String utilisateurId,
        String titre,
        String message,
        NotificationType type,
        NotificationNiveau niveau,
        boolean lu,
        Instant dateCreation,
        String lienAction
    ) {
    }

    public record NotificationListResponse(
        long unreadCount,
        List<NotificationResponse> notifications
    ) {
    }

    public record CreateNotificationRequest(
        String utilisateurId,
        @NotBlank String titre,
        @NotBlank String message,
        @NotNull NotificationType type,
        @NotNull NotificationNiveau niveau,
        String lienAction
    ) {
    }
}
