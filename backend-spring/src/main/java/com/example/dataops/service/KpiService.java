package com.example.dataops.service;

import com.example.dataops.dto.KpiDtos;
import com.example.dataops.dto.StockDtos;
import com.example.dataops.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Service
public class KpiService {
    private final SaleRepository saleRepository;
    private final StockService stockService;
    private final AlertService alertService;

    public KpiService(SaleRepository saleRepository, StockService stockService, AlertService alertService) {
        this.saleRepository = saleRepository;
        this.stockService = stockService;
        this.alertService = alertService;
    }

    @Transactional(readOnly = true)
    public KpiDtos.KpiOverviewResponse overview() {
        List<KpiDtos.MetricResponse> revenueByAgency = saleRepository.revenueByAgency().stream()
            .map(row -> new KpiDtos.MetricResponse((String) row[0], toBigDecimal(row[1])))
            .toList();

        List<KpiDtos.MetricResponse> unitsByProduct = saleRepository.unitsByProduct().stream()
            .map(row -> new KpiDtos.MetricResponse((String) row[0], toBigDecimal(row[1])))
            .toList();

        List<StockDtos.StockLevelResponse> stockLevels = stockService.stockLevels();
        return new KpiDtos.KpiOverviewResponse(
            saleRepository.totalRevenue(),
            saleRepository.totalUnitsSold(),
            saleRepository.count(),
            alertService.activeCount(),
            revenueByAgency,
            unitsByProduct,
            stockLevels
        );
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
}

