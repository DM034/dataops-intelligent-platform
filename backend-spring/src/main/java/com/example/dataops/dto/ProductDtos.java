package com.example.dataops.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public final class ProductDtos {
    private ProductDtos() {
    }

    public record ProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        String category,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
        Boolean active
    ) {
    }

    public record ProductResponse(
        Long id,
        String sku,
        String name,
        String category,
        BigDecimal unitPrice,
        boolean active,
        Instant createdAt
    ) {
    }
}

