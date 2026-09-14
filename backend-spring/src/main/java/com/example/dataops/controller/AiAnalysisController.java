package com.example.dataops.controller;

import com.example.dataops.dto.AiDtos;
import com.example.dataops.model.JournalNiveau;
import com.example.dataops.service.AiAnalysisService;
import com.example.dataops.service.AiBenchmarkService;
import com.example.dataops.service.AiDecisionIntelligenceService;
import com.example.dataops.service.JournalActiviteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {
    private final AiAnalysisService aiAnalysisService;
    private final AiBenchmarkService aiBenchmarkService;
    private final AiDecisionIntelligenceService aiDecisionIntelligenceService;
    private final JournalActiviteService journalActiviteService;

    public AiAnalysisController(
        AiAnalysisService aiAnalysisService,
        AiBenchmarkService aiBenchmarkService,
        AiDecisionIntelligenceService aiDecisionIntelligenceService,
        JournalActiviteService journalActiviteService
    ) {
        this.aiAnalysisService = aiAnalysisService;
        this.aiBenchmarkService = aiBenchmarkService;
        this.aiDecisionIntelligenceService = aiDecisionIntelligenceService;
        this.journalActiviteService = journalActiviteService;
    }

    @GetMapping("/sales-anomalies")
    public AiDtos.SalesAnomalyAnalysisResponse salesAnomalies() {
        AiDtos.SalesAnomalyAnalysisResponse response = aiAnalysisService.analyzeSalesAnomalies();
        journalActiviteService.journaliser(JournalNiveau.INFO, "APPEL_MODULE_PREDICTIF", "IA", "Analyse predictive des anomalies de ventes", "resultCount=" + response.results().size(), "SALES_ANOMALIES");
        return response;
    }

    @GetMapping("/stock-predictions")
    public AiDtos.StockPredictionAnalysisResponse stockPredictions() {
        AiDtos.StockPredictionAnalysisResponse response = aiAnalysisService.analyzeStockPredictions();
        journalActiviteService.journaliser(JournalNiveau.INFO, "APPEL_MODULE_PREDICTIF", "IA", "Prediction de rupture de stock", "resultCount=" + response.results().size(), "STOCK_PREDICTIONS");
        return response;
    }

    @GetMapping("/benchmark/anomalies")
    public AiDtos.BenchmarkAnomalyResponse benchmarkAnomalies() {
        AiDtos.BenchmarkAnomalyResponse response = aiBenchmarkService.benchmarkSalesAnomalies();
        journalActiviteService.journaliser(JournalNiveau.INFO, "APPEL_MODULE_PREDICTIF", "IA", "Benchmark des methodes d'anomalies", "recommendedMethod=" + response.recommendedMethod(), "BENCHMARK_ANOMALIES");
        return response;
    }

    @GetMapping("/decision-intelligence")
    public AiDtos.DecisionIntelligenceResponse decisionIntelligence() {
        AiDtos.DecisionIntelligenceResponse response = aiDecisionIntelligenceService.analyze();
        journalActiviteService.journaliser(JournalNiveau.INFO, "APPEL_MODULE_PREDICTIF", "IA", "Analyse decisionnelle avancee", "moduleCount=15", "DECISION_INTELLIGENCE");
        return response;
    }

    @GetMapping("/models/status")
    public Map<String, Object> modelsStatus() {
        return aiDecisionIntelligenceService.modelsStatus();
    }
}
