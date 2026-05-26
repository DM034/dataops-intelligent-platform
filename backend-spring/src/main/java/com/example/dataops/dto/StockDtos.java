package com.example.dataops.dto;

import com.example.dataops.model.StockMovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class StockDtos {
    private StockDtos() {
    }

    public record StockMovementRequest(
        @NotNull Long agencyId,
        @NotNull Long productId,
        @NotNull StockMovementType type,
        @NotNull @Min(1) Integer quantity,
        LocalDateTime movementDate,
        String reason
    ) {
    }

    public record StockMovementResponse(
        Long id,
        Long agencyId,
        String agencyName,
        Long productId,
        String productName,
        StockMovementType type,
        Integer quantity,
        LocalDateTime movementDate,
        String reason
    ) {
    }

    public record StockLevelResponse(String productName, String agencyName, Long quantity) {
    }
}

