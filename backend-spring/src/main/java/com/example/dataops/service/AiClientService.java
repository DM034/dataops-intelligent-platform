package com.example.dataops.service;

import com.example.dataops.dto.AiDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiClientService {
    private final RestClient restClient;

    public AiClientService(@Value("${services.ai.url}") String aiServiceUrl) {
        this.restClient = RestClient.builder()
            .baseUrl(aiServiceUrl)
            .build();
    }

    public AiDtos.SalesAnomalyResponse detectSalesAnomalies(AiDtos.SalesAnomalyRequest request) {
        return restClient.post()
            .uri("/ai/anomalies/sales")
            .body(request)
            .retrieve()
            .body(AiDtos.SalesAnomalyResponse.class);
    }

    public AiDtos.StockPredictionResponse predictStock(AiDtos.StockPredictionRequest request) {
        return restClient.post()
            .uri("/ai/stock/predict")
            .body(request)
            .retrieve()
            .body(AiDtos.StockPredictionResponse.class);
    }
}
