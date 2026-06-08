package com.example.dataops.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class DataGovernanceDtos {
    private DataGovernanceDtos() {
    }

    public record DataQualityReportResponse(
        Long id,
        String importFileId,
        String sourceName,
        Integer totalRows,
        Integer validRows,
        Integer errorRows,
        Integer duplicateRecords,
        BigDecimal completenessRate,
        BigDecimal validityRate,
        BigDecimal uniquenessRate,
        BigDecimal consistencyRate,
        BigDecimal globalScore,
        Integer totalRecords,
        Integer validRecords,
        Integer invalidRecords,
        BigDecimal qualityScore,
        Instant createdAt
    ) {
    }

    public record DataLineageResponse(
        Long id,
        String sourceName,
        String source,
        String sourceType,
        Instant importDate,
        Instant validationDate,
        Instant transformationDate,
        Instant storageDate,
        Instant dashboardDate,
        String validationStep,
        String transformationStep,
        String storageStep,
        String dashboardStep,
        String status
    ) {
    }

    public record DataCatalogRequest(
        @NotBlank String name,
        @NotBlank String sourceType,
        @NotBlank String description,
        @NotBlank String owner,
        @NotBlank String refreshFrequency
    ) {
    }

    public record DataCatalogResponse(
        Long id,
        String name,
        String sourceType,
        String description,
        String owner,
        String refreshFrequency,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record ImportAuditResponse(
        Long id,
        String fileName,
        String importedBy,
        Instant importDate,
        Integer totalRows,
        Integer successRows,
        Integer failedRows,
        String status
    ) {
    }

    public record GovernanceDashboardResponse(
        BigDecimal globalQualityScore,
        Integer errorCount,
        Integer duplicateCount,
        Integer importCount,
        BigDecimal completenessRate,
        BigDecimal validityRate,
        List<DataQualityReportResponse> qualityHistory,
        List<DataLineageResponse> lineage,
        List<DataCatalogResponse> catalog,
        List<ImportAuditResponse> imports
    ) {
    }
}
