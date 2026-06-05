package com.example.dataops.dto;

import java.util.List;

public final class ImportDtos {
    private ImportDtos() {
    }

    public record ImportResultResponse(int importedRows, int skippedRows, List<ImportLineError> errors, Long dataQualityReportId, Long dataLineageId) {
    }

    public record ImportLineError(long line, String message) {
    }
}
