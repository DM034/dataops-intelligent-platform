package com.example.dataops.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class BlockchainDtos {
    private BlockchainDtos() {
    }

    public record AuditEventRequest(
        @NotBlank String action,
        @NotBlank String entityType,
        Long entityId,
        @NotBlank String userId,
        @NotBlank String data
    ) {
    }

    public record BlockchainBlockResponse(
        Long id,
        Instant timestamp,
        String action,
        String entityType,
        Long entityId,
        String userId,
        String dataHash,
        String previousHash,
        String currentHash
    ) {
    }

    public record ChainValidationResponse(boolean valid, String message) {
    }
}
