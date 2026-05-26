package com.example.dataops.dto;

public final class ImportDtos {
    private ImportDtos() {
    }

    public record ImportResultResponse(int importedRows, int skippedRows) {
    }
}

