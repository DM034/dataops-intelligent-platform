package com.example.dataops.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.dataops.model.AlertSeverity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class AiDtos {
    private AiDtos() {
    }

    public record AiAlert(String level, AlertSeverity severity, Long alertId, String title, String message) {
    }

    public record SalePoint(LocalDate date, String agencyCode, String productCode, Integer quantity, BigDecimal unitPrice) {
    }

    public record SalesAnomalyRequest(List<SalePoint> sales, @JsonProperty("zscore_threshold") double zscoreThreshold) {
    }

    public record SalesAnomalyResult(
        LocalDate date,
        String agencyCode,
        String productCode,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal revenue,
        double movingAverage7Days,
        double zScore,
        boolean anomaly,
        String alertLevel
    ) {
    }

    public record SalesAnomalyResponse(
        int count,
        int anomalyCount,
        double zscoreThreshold,
        List<SalesAnomalyResult> results
    ) {
    }

    public record SalesAnomalyAnalysisResponse(
        int count,
        int anomalyCount,
        int createdAlertCount,
        List<AiAlert> createdAlerts,
        List<SalesAnomalyResult> results
    ) {
    }

    public record StockHistoryPoint(LocalDate date, long stockLevel) {
    }

    public record StockPredictionRequest(
        String productCode,
        String agencyCode,
        long currentStock,
        long reorderThreshold,
        List<StockHistoryPoint> history
    ) {
    }

    public record StockPredictionResponse(
        String productCode,
        String agencyCode,
        long currentStock,
        long reorderThreshold,
        double averageDailyConsumption,
        Integer predictedDaysToStockout,
        LocalDate stockoutDate,
        String alertLevel,
        String recommendation
    ) {
    }

    public record StockPredictionAnalysisResponse(
        int analyzedCount,
        int skippedCount,
        int createdAlertCount,
        List<AiAlert> createdAlerts,
        List<StockPredictionResponse> results
    ) {
    }

    public record BenchmarkSalePoint(String date, String agencyCode, String productCode, Integer quantity, BigDecimal amount) {
    }

    public record BenchmarkAnomaly(
        String date,
        String agencyCode,
        String productCode,
        Integer quantity,
        BigDecimal amount,
        double score,
        String reason
    ) {
    }

    public record BenchmarkMethodResult(
        List<BenchmarkAnomaly> anomalies,
        double executionTimeMs,
        int anomalyCount
    ) {
    }

    public record BenchmarkAnomalyResponse(
        BenchmarkMethodResult zscore,
        BenchmarkMethodResult iqr,
        BenchmarkMethodResult movingAverage,
        String recommendedMethod
    ) {
    }

    public record DecisionSalePoint(LocalDate date, String agencyCode, String productCode, Integer quantity, BigDecimal amount) {
    }

    public record DecisionStockPoint(LocalDate date, String agencyCode, String productCode, Integer quantity, String type) {
    }

    public record SourceQualitySnapshot(String sourceName, BigDecimal qualityScore, Integer totalRows, Integer errorRows) {
    }

    public record WhatIfScenario(double demandIncreasePercent, int supplierDelayDays) {
    }

    public record DecisionIntelligenceRequest(
        List<DecisionSalePoint> sales,
        List<DecisionStockPoint> stocks,
        List<SourceQualitySnapshot> sourceScores,
        WhatIfScenario whatIf
    ) {
    }

    public record DecisionIntelligenceResponse(
        Map<String, Object> summary,
        Map<String, Object> modules,
        List<Map<String, Object>> risks,
        List<Map<String, Object>> recommendations,
        Map<String, Object> modelStatus
    ) {
    }
}
