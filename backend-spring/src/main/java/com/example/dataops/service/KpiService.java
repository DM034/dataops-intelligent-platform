package com.example.dataops.service;

import com.example.dataops.dto.KpiDtos;
import com.example.dataops.dto.StockDtos;
import com.example.dataops.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

@Service
public class KpiService {
    private final SaleRepository saleRepository;
    private final StockService stockService;
    private final RegleMetierService regleMetierService;

    public KpiService(SaleRepository saleRepository, StockService stockService, RegleMetierService regleMetierService) {
        this.saleRepository = saleRepository;
        this.stockService = stockService;
        this.regleMetierService = regleMetierService;
    }

    @Transactional(readOnly = true)
    public KpiDtos.KpiOverviewResponse overview() {
        List<StockDtos.StockLevelResponse> stockLevels = stockService.stockLevels();
        return new KpiDtos.KpiOverviewResponse(
            saleRepository.totalRevenue(),
            saleRepository.count(),
            totalStock(stockLevels),
            (long) criticalStocks(stockLevels).size(),
            salesByAgency(),
            salesByProduct(),
            dailySales()
        );
    }

    @Transactional(readOnly = true)
    public List<KpiDtos.MetricResponse> salesByAgency() {
        return saleRepository.revenueByAgency().stream()
            .map(row -> new KpiDtos.MetricResponse((String) row[0], toBigDecimal(row[1])))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<KpiDtos.MetricResponse> salesByProduct() {
        return saleRepository.unitsByProduct().stream()
            .map(row -> new KpiDtos.MetricResponse((String) row[0], toBigDecimal(row[1])))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<StockDtos.StockLevelResponse> criticalStocks() {
        return criticalStocks(stockService.stockLevels());
    }

    @Transactional(readOnly = true)
    public List<KpiDtos.DailySalesResponse> dailySales() {
        return saleRepository.dailySales().stream()
            .map(row -> new KpiDtos.DailySalesResponse(
                (LocalDate) row[0],
                toBigDecimal(row[1]),
                toLong(row[2]),
                toLong(row[3])
            ))
            .toList();
    }

    private Long totalStock(List<StockDtos.StockLevelResponse> stockLevels) {
        return stockLevels.stream()
            .mapToLong(StockDtos.StockLevelResponse::quantity)
            .sum();
    }

    private List<StockDtos.StockLevelResponse> criticalStocks(List<StockDtos.StockLevelResponse> stockLevels) {
        long threshold = regleMetierService.getDecimal(RegleMetierService.STOCK_CRITIQUE, BigDecimal.TEN).longValue();
        return stockLevels.stream()
            .filter(stock -> stock.quantity() <= threshold)
            .toList();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal typed) {
            return typed;
        }
        if (value instanceof BigInteger typed) {
            return new BigDecimal(typed);
        }
        if (value instanceof Number typed) {
            return BigDecimal.valueOf(typed.longValue());
        }
        return BigDecimal.ZERO;
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
}
