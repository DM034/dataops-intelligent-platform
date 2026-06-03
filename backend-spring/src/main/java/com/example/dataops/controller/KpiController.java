package com.example.dataops.controller;

import com.example.dataops.dto.KpiDtos;
import com.example.dataops.dto.StockDtos;
import com.example.dataops.service.KpiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kpi")
public class KpiController {
    private final KpiService service;

    public KpiController(KpiService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public KpiDtos.KpiOverviewResponse overview() {
        return service.overview();
    }

    @GetMapping("/sales-by-agency")
    public List<KpiDtos.MetricResponse> salesByAgency() {
        return service.salesByAgency();
    }

    @GetMapping("/sales-by-product")
    public List<KpiDtos.MetricResponse> salesByProduct() {
        return service.salesByProduct();
    }

    @GetMapping("/critical-stocks")
    public List<StockDtos.StockLevelResponse> criticalStocks() {
        return service.criticalStocks();
    }

    @GetMapping("/daily-sales")
    public List<KpiDtos.DailySalesResponse> dailySales() {
        return service.dailySales();
    }
}
