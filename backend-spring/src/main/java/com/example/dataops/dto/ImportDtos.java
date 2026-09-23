package com.example.dataops.dto;

import java.time.Instant;
import java.util.List;

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
}
