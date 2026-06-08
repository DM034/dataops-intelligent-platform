package com.example.dataops.service;

import com.example.dataops.dto.BlockchainDtos;
import com.example.dataops.dto.HealthDtos;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class HealthService {
    private final DataSource dataSource;
    private final AiClientService aiClientService;
    private final BlockchainService blockchainService;

    public HealthService(DataSource dataSource, AiClientService aiClientService, BlockchainService blockchainService) {
        this.dataSource = dataSource;
        this.aiClientService = aiClientService;
        this.blockchainService = blockchainService;
    }

    public HealthDtos.DependencyHealthResponse dependencies() {
        List<HealthDtos.DependencyStatus> dependencies = new ArrayList<>();
        dependencies.add(checkPostgres());
        dependencies.add(checkAiService());
        dependencies.add(checkBlockchain());

        boolean allAvailable = dependencies.stream()
            .allMatch(dependency -> "OK".equals(dependency.status()));

        return new HealthDtos.DependencyHealthResponse(
            allAvailable ? "OK" : "DEGRADED",
            Instant.now(),
            dependencies
        );
    }

    private HealthDtos.DependencyStatus checkPostgres() {
        try (Connection connection = dataSource.getConnection()) {
            return new HealthDtos.DependencyStatus("postgres", connection.isValid(2) ? "OK" : "KO", "Database connection checked");
        } catch (Exception exception) {
            return new HealthDtos.DependencyStatus("postgres", "KO", exception.getMessage());
        }
    }

    private HealthDtos.DependencyStatus checkAiService() {
        try {
            String status = aiClientService.healthStatus();
            return new HealthDtos.DependencyStatus("ai-service", "ok".equalsIgnoreCase(status) ? "OK" : "KO", "FastAPI status=" + status);
        } catch (Exception exception) {
            return new HealthDtos.DependencyStatus("ai-service", "KO", exception.getMessage());
        }
    }

    private HealthDtos.DependencyStatus checkBlockchain() {
        try {
            BlockchainDtos.ChainValidationResponse response = blockchainService.verifyChain();
            return new HealthDtos.DependencyStatus("blockchain", response.valid() ? "OK" : "KO", response.message());
        } catch (Exception exception) {
            return new HealthDtos.DependencyStatus("blockchain", "KO", exception.getMessage());
        }
    }
}
