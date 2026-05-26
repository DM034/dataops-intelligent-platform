package com.example.dataops.controller;

import com.example.dataops.dto.KpiDtos;
import com.example.dataops.service.KpiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpis")
public class KpiController {
    private final KpiService service;

    public KpiController(KpiService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public KpiDtos.KpiOverviewResponse overview() {
        return service.overview();
    }
}

