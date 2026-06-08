package com.example.dataops.controller;

import com.example.dataops.dto.DataGovernanceDtos;
import com.example.dataops.service.DataGovernanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/data-quality")
public class DataQualityController {
    private final DataGovernanceService service;

    public DataQualityController(DataGovernanceService service) {
        this.service = service;
    }

    @GetMapping("/reports")
    public List<DataGovernanceDtos.DataQualityReportResponse> reports() {
        return service.reports();
    }

    @GetMapping
    public List<DataGovernanceDtos.DataQualityReportResponse> quality() {
        return service.reports();
    }

    @GetMapping("/latest")
    public DataGovernanceDtos.DataQualityReportResponse latest() {
        return service.latestReport();
    }

    @GetMapping("/history")
    public List<DataGovernanceDtos.DataQualityReportResponse> history() {
        return service.reports();
    }

    @GetMapping("/reports/{id}")
    public DataGovernanceDtos.DataQualityReportResponse report(@PathVariable Long id) {
        return service.report(id);
    }
}
