package com.example.dataops.dto;

import java.math.BigDecimal;
import java.time.Instant;

public final class DataGovernanceDtos {
    private DataGovernanceDtos() {
    }

    public record DataQualityReportResponse(
        Long id,
        String importFileId,
        Integer totalRows,
        Integer validRows,
        Integer errorRows,
        BigDecimal completenessRate,
        BigDecimal validityRate,
        BigDecimal uniquenessRate,
        BigDecimal consistencyRate,
        BigDecimal globalScore,
        Instant createdAt
    ) {
    }

    public record DataLineageResponse(
        Long id,
        String sourceName,
        String sourceType,
        Instant importDate,
        String validationStep,
        String transformationStep,
        String storageStep,
        String dashboardStep,
        String status
    ) {
    }
}
