package com.example.dataops.controller;

import com.example.dataops.dto.HealthDtos;
import com.example.dataops.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public HealthDtos.HealthResponse health() {
        return new HealthDtos.HealthResponse("ok", "backend-spring", Instant.now());
    }

    @GetMapping("/health/dependencies")
    public HealthDtos.DependencyHealthResponse dependencies() {
        return healthService.dependencies();
    }
}
