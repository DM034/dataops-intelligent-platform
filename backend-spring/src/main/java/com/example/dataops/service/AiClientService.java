package com.example.dataops.service;

import com.example.dataops.dto.AiDtos;
import com.example.dataops.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class AiClientService {
    private final RestClient restClient;

    public AiClientService(@Value("${services.ai.url}") String aiServiceUrl) {
        this.restClient = RestClient.builder()
            .baseUrl(aiServiceUrl)
            .build();
    }

    public AiDtos.SalesAnomalyResponse detectSalesAnomalies(AiDtos.SalesAnomalyRequest request) {
        try {
            return restClient.post()
                .uri("/ai/anomalies/sales")
                .body(request)
                .retrieve()
                .body(AiDtos.SalesAnomalyResponse.class);
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    public AiDtos.StockPredictionResponse predictStock(AiDtos.StockPredictionRequest request) {
        try {
            return restClient.post()
                .uri("/ai/stock/predict")
                .body(request)
                .retrieve()
                .body(AiDtos.StockPredictionResponse.class);
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    public AiDtos.BenchmarkAnomalyResponse benchmarkAnomalies(List<AiDtos.BenchmarkSalePoint> sales) {
        try {
            return restClient.post()
                .uri("/ai/benchmark/anomalies")
                .body(sales)
                .retrieve()
                .body(AiDtos.BenchmarkAnomalyResponse.class);
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    public String healthStatus() {
        try {
            Map<?, ?> response = restClient.get()
                .uri("/health")
                .retrieve()
                .body(Map.class);
            Object status = response == null ? null : response.get("status");
            return status == null ? "unknown" : status.toString();
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    private BusinessException unavailable(RestClientException exception) {
        return new BusinessException("AI service unavailable: " + exception.getMessage());
    }
}
