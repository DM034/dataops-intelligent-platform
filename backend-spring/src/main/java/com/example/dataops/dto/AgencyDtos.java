package com.example.dataops.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class AgencyDtos {
    private AgencyDtos() {
    }

    public record AgencyRequest(@NotBlank String code, @NotBlank String name, String city, Boolean active) {
    }

    public record AgencyResponse(Long id, String code, String name, String city, boolean active, Instant createdAt) {
    }
}

