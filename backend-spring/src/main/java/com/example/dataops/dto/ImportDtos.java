package com.example.dataops.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ImportDtos {
    private ImportDtos() {
    }

    public record ImportResultResponse(int importedRows, int skippedRows, List<ImportLineError> errors, Long dataQualityReportId, Long dataLineageId) {
    }

    public record ImportLineError(long line, String message) {
    }

    public record ImportJobStartedResponse(String jobId, String status, int progressPercent, long totalRows) {
    }

    public record ImportPreviewResponse(
        String detectedType,
        String status,
        long totalRows,
        List<String> headers,
        List<String> missingColumns,
        List<ImportLineError> sampleErrors,
        String message
    ) {
    }

    public record ImportJobProgressResponse(
        String jobId,
        String type,
        String fileName,
        String mode,
        String status,
        int progressPercent,
        long totalRows,
        long processedRows,
        int importedRows,
        int skippedRows,
        int errorCount,
        String message,
        double rowsPerSecond,
        Long estimatedRemainingSeconds,
        String errorDownloadUrl,
        ImportResultResponse result,
        Instant startedAt,
        Instant finishedAt
    ) {
    }

    public record ImportJobSummaryResponse(
        String jobId,
        String type,
        String fileName,
        String mode,
        String status,
        int progressPercent,
        long totalRows,
        long processedRows,
        int importedRows,
        int skippedRows,
        int errorCount,
        String message,
        double rowsPerSecond,
        Long estimatedRemainingSeconds,
        String errorDownloadUrl,
        Instant startedAt,
        Instant finishedAt
    ) {
    }

    public record DatasetVersionResponse(
        Long id,
        String datasetName,
        String importJobId,
        String sourceFileName,
        String sourceFileHash,
        String schemaVersion,
        String status,
        Object qualityScore,
        Instant createdAt
    ) {
    }

    public record DataContractResponse(String type, String schemaVersion, List<String> requiredColumns, Map<String, String> columnTypes, List<String> businessRules) {
    }

    public record ImportObservabilityResponse(
        long totalJobs,
        long runningJobs,
        long failedJobs,
        long completedJobs,
        double averageRowsPerSecond,
        List<String> warnings
    ) {
    }

    public record DataQualityAlertResponse(String severity, String type, String message, String importJobId) {
    }

    public record RollbackResponse(String jobId, String status, long deletedRows, String message) {
    }
}
