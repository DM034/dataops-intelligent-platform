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

    public record ImportJobProgressResponse(
        String jobId,
        String status,
        int progressPercent,
        long totalRows,
        long processedRows,
        int importedRows,
        int skippedRows,
        String message,
        ImportResultResponse result,
        Instant startedAt,
        Instant finishedAt
    ) {
    }
}
