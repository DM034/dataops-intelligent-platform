package com.example.dataops.controller;

import com.example.dataops.dto.DataGovernanceDtos;
import com.example.dataops.service.DataGovernanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/governance")
public class GovernanceDashboardController {
    private final DataGovernanceService service;

    public GovernanceDashboardController(DataGovernanceService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public DataGovernanceDtos.GovernanceDashboardResponse dashboard() {
        return service.dashboard();
    }
}
