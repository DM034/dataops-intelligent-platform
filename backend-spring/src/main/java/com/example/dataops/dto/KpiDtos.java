package com.example.dataops.dto;

import java.math.BigDecimal;
import java.util.List;

public final class KpiDtos {
    private KpiDtos() {
    }

    public record KpiOverviewResponse(
        BigDecimal totalRevenue,
        Long totalUnitsSold,
        Long totalSales,
        Long activeAlerts,
        List<MetricResponse> revenueByAgency,
        List<MetricResponse> unitsByProduct,
        List<StockDtos.StockLevelResponse> stockLevels
    ) {
    }

    public record MetricResponse(String label, BigDecimal value) {
    }
}

