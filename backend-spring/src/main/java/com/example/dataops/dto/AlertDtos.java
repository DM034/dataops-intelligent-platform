package com.example.dataops.dto;

import com.example.dataops.model.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class AlertDtos {
    private AlertDtos() {
    }

    public record AlertRequest(@NotNull AlertSeverity severity, @NotBlank String title, @NotBlank String message) {
    }

    public record AlertResponse(Long id, AlertSeverity severity, String title, String message, boolean resolved, Instant createdAt) {
    }
}

