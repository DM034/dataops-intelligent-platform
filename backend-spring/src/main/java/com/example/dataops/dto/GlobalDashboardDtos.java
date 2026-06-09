package com.example.dataops.dto;

import com.example.dataops.model.AlertSeverity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class GlobalDashboardDtos {
    private GlobalDashboardDtos() {
    }

    public record DashboardGlobalResponse(
        KpiCards kpis,
        List<PeriodMetric> nonConformitiesTrend,
        List<StatusMetric> productionOrdersByStatus,
        List<CategoryStockMetric> stockByProductCategory,
        List<RecentAlert> recentAlerts,
        String dataMode
    ) {
    }

    public record KpiCards(
        long totalProductionOrders,
        long delayedProductionOrders,
        BigDecimal nonConformityRate,
        long criticalStockProducts,
        long recommendedPurchases,
        long activeAlerts
    ) {
    }

    public record PeriodMetric(String period, long value) {
    }

    public record StatusMetric(String status, long value) {
    }

    public record CategoryStockMetric(String category, long stockLevel) {
    }

    public record RecentAlert(
        Long id,
        AlertSeverity severity,
        String title,
        String message,
        Instant createdAt
    ) {
    }
}
