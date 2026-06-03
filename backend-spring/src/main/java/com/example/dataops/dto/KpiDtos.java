package com.example.dataops.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class KpiDtos {
    private KpiDtos() {
    }

    public record KpiOverviewResponse(
        BigDecimal totalRevenue,
        Long totalSales,
        Long totalStock,
        Long criticalStockProducts,
        List<MetricResponse> salesByAgency,
        List<MetricResponse> topProducts,
        List<DailySalesResponse> dailySales
    ) {
    }

    public record MetricResponse(String label, BigDecimal value) {
    }

    public record DailySalesResponse(LocalDate date, BigDecimal revenue, Long salesCount, Long unitsSold) {
    }
}
