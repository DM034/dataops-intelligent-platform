package com.example.dataops.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class BlockchainDtos {
    private BlockchainDtos() {
    }

    public record AuditEventRequest(@NotBlank String action, @NotBlank String actor, @NotBlank String payload) {
    }

    public record BlockchainBlockResponse(
        Long id,
        Long blockIndex,
        Instant timestamp,
        String action,
        String actor,
        String payload,
        String previousHash,
        String hash
    ) {
    }

    public record ChainValidationResponse(boolean valid, String message) {
    }
}

