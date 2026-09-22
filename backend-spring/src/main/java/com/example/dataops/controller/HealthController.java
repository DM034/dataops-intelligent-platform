package com.example.dataops.controller;

import com.example.dataops.dto.BlockchainDtos;
import com.example.dataops.service.AiDecisionIntelligenceService;
import com.example.dataops.service.BlockchainService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    private final DataSource dataSource;
    private final AiDecisionIntelligenceService aiDecisionIntelligenceService;
    private final BlockchainService blockchainService;

    public HealthController(
        DataSource dataSource,
        AiDecisionIntelligenceService aiDecisionIntelligenceService,
        BlockchainService blockchainService
    ) {
        this.dataSource = dataSource;
        this.aiDecisionIntelligenceService = aiDecisionIntelligenceService;
        this.blockchainService = blockchainService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "backend-spring");
    }

    @GetMapping("/health/dependencies")
    public Map<String, Object> dependencies() {
        Map<String, Object> dependencies = new LinkedHashMap<>();
        dependencies.put("backend", dependency("OK", "backend-spring disponible"));

        boolean databaseOk = isDatabaseAvailable();
        dependencies.put("database", dependency(databaseOk ? "OK" : "KO", databaseOk ? "PostgreSQL disponible" : "PostgreSQL indisponible"));

        Map<String, Object> aiStatus = aiDecisionIntelligenceService.modelsStatus();
        boolean aiOk = !"unavailable".equalsIgnoreCase(String.valueOf(aiStatus.get("status")));
        dependencies.put("aiService", dependency(aiOk ? "OK" : "KO", aiOk ? "FastAPI disponible" : "FastAPI indisponible"));

        BlockchainDtos.ChainValidationResponse chainStatus = blockchainService.verifyChain();
        dependencies.put("blockchain", Map.of(
            "status", "OK",
            "message", "Blockchain interne disponible",
            "chainValid", chainStatus.valid(),
            "chainMessage", chainStatus.message()
        ));

        boolean allOk = databaseOk && aiOk;
        return Map.of(
            "status", allOk ? "OK" : "DEGRADED",
            "dependencies", dependencies
        );
    }

    private boolean isDatabaseAvailable() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception exception) {
            return false;
        }
    }

    private Map<String, String> dependency(String status, String message) {
        return Map.of("status", status, "message", message);
    }
}
