package com.example.dataops.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class SaleDtos {
    private SaleDtos() {
    }

    public record SaleRequest(
        @NotNull Long agencyId,
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        BigDecimal unitPrice,
        @NotNull LocalDate saleDate,
        String reference
    ) {
    }

    public record SaleResponse(
        Long id,
        Long agencyId,
        String agencyName,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        LocalDate saleDate,
        String reference
    ) {
    }
}

