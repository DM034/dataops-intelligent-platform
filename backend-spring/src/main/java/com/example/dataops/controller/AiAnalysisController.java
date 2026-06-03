package com.example.dataops.controller;

import com.example.dataops.dto.AiDtos;
import com.example.dataops.service.AiAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {
    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @GetMapping("/sales-anomalies")
    public AiDtos.SalesAnomalyAnalysisResponse salesAnomalies() {
        return aiAnalysisService.analyzeSalesAnomalies();
    }

    @GetMapping("/stock-predictions")
    public AiDtos.StockPredictionAnalysisResponse stockPredictions() {
        return aiAnalysisService.analyzeStockPredictions();
    }
}
