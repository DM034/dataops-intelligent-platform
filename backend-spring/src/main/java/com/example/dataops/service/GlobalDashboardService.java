package com.example.dataops.service;

import com.example.dataops.dto.GlobalDashboardDtos;
import com.example.dataops.model.Alert;
import com.example.dataops.model.Product;
import com.example.dataops.model.RecommendationStatus;
import com.example.dataops.repository.AlertRepository;
import com.example.dataops.repository.RecommendationRepository;
import com.example.dataops.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GlobalDashboardService {
    private final AlertRepository alertRepository;
    private final RecommendationRepository recommendationRepository;
    private final StockMovementRepository stockMovementRepository;
    private final RegleMetierService regleMetierService;

    public GlobalDashboardService(
        AlertRepository alertRepository,
        RecommendationRepository recommendationRepository,
        StockMovementRepository stockMovementRepository,
        RegleMetierService regleMetierService
    ) {
        this.alertRepository = alertRepository;
        this.recommendationRepository = recommendationRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.regleMetierService = regleMetierService;
    }

    @Transactional(readOnly = true)
    public GlobalDashboardDtos.DashboardGlobalResponse dashboard() {
        List<Alert> activeAlerts = alertRepository.findByResolvedFalseOrderByCreatedAtDesc();
        List<StockLevel> stockLevels = stockLevels();
        long recommendedPurchases = recommendationRepository.findAll().stream()
            .filter(recommendation -> recommendation.getStatus() == RecommendationStatus.NEW || recommendation.getStatus() == RecommendationStatus.IN_PROGRESS)
            .count();

        long totalProductionOrders = 128;
        long delayedProductionOrders = 14;
        long totalQualityControls = 450;
        long nonConformities = 27;
        long criticalStockThreshold = regleMetierService.getDecimal(RegleMetierService.STOCK_CRITIQUE, BigDecimal.TEN).longValue();

        GlobalDashboardDtos.KpiCards kpis = new GlobalDashboardDtos.KpiCards(
            totalProductionOrders,
            delayedProductionOrders,
            rate(nonConformities, totalQualityControls),
            stockLevels.stream().filter(stock -> stock.stockLevel() <= criticalStockThreshold).count(),
            recommendedPurchases,
            activeAlerts.size()
        );

        return new GlobalDashboardDtos.DashboardGlobalResponse(
            kpis,
            mockedNonConformitiesTrend(),
            mockedProductionStatuses(),
            stockByCategory(stockLevels),
            activeAlerts.stream()
                .limit(8)
                .map(this::toRecentAlert)
                .toList(),
            "MIXED_REAL_AND_MOCKED_AGGREGATES"
        );
    }

    private List<GlobalDashboardDtos.PeriodMetric> mockedNonConformitiesTrend() {
        return List.of(
            new GlobalDashboardDtos.PeriodMetric("Jan", 6),
            new GlobalDashboardDtos.PeriodMetric("Fév", 8),
            new GlobalDashboardDtos.PeriodMetric("Mar", 5),
            new GlobalDashboardDtos.PeriodMetric("Avr", 9),
            new GlobalDashboardDtos.PeriodMetric("Mai", 4),
            new GlobalDashboardDtos.PeriodMetric("Juin", 7)
        );
    }

    private List<GlobalDashboardDtos.StatusMetric> mockedProductionStatuses() {
        return List.of(
            new GlobalDashboardDtos.StatusMetric("PLANIFIÉ", 34),
            new GlobalDashboardDtos.StatusMetric("EN_COURS", 41),
            new GlobalDashboardDtos.StatusMetric("TERMINÉ", 39),
            new GlobalDashboardDtos.StatusMetric("EN_RETARD", 14)
        );
    }

    private List<GlobalDashboardDtos.CategoryStockMetric> stockByCategory(List<StockLevel> stockLevels) {
        Map<String, Long> grouped = new LinkedHashMap<>();
        for (StockLevel stock : stockLevels) {
            grouped.merge(stock.category(), stock.stockLevel(), Long::sum);
        }
        if (grouped.isEmpty()) {
            grouped.put("Matières premières", 320L);
            grouped.put("Produits finis", 185L);
            grouped.put("Emballages", 74L);
            grouped.put("Maintenance", 42L);
        }
        return grouped.entrySet().stream()
            .map(entry -> new GlobalDashboardDtos.CategoryStockMetric(entry.getKey(), entry.getValue()))
            .toList();
    }

    private List<StockLevel> stockLevels() {
        List<StockLevel> levels = new ArrayList<>();
        for (Object[] row : stockMovementRepository.stockLevelsByEntity()) {
            Product product = (Product) row[0];
            levels.add(new StockLevel(
                product.getCategory() == null || product.getCategory().isBlank() ? "Sans catégorie" : product.getCategory(),
                toLong(row[2])
            ));
        }
        return levels;
    }

    private GlobalDashboardDtos.RecentAlert toRecentAlert(Alert alert) {
        return new GlobalDashboardDtos.RecentAlert(
            alert.getId(),
            alert.getSeverity(),
            alert.getTitle(),
            alert.getMessage(),
            alert.getCreatedAt()
        );
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private Long toLong(Object value) {
        if (value instanceof Long typed) {
            return typed;
        }
        if (value instanceof Integer typed) {
            return typed.longValue();
        }
        if (value instanceof BigInteger typed) {
            return typed.longValue();
        }
        if (value instanceof Number typed) {
            return typed.longValue();
        }
        return 0L;
    }

    private record StockLevel(String category, Long stockLevel) {
    }
}
