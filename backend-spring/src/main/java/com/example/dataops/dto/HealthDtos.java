package com.example.dataops.dto;

import java.time.Instant;
import java.util.List;

public final class HealthDtos {
    private HealthDtos() {
    }

    public record HealthResponse(String status, String service, Instant timestamp) {
    }

    public record DependencyHealthResponse(String status, Instant timestamp, List<DependencyStatus> dependencies) {
    }

    public record DependencyStatus(String name, String status, String details) {
    }
}
