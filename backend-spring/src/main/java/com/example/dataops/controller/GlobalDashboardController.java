package com.example.dataops.controller;

import com.example.dataops.dto.GlobalDashboardDtos;
import com.example.dataops.service.GlobalDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class GlobalDashboardController {
    private final GlobalDashboardService service;

    public GlobalDashboardController(GlobalDashboardService service) {
        this.service = service;
    }

    @GetMapping("/global")
    public GlobalDashboardDtos.DashboardGlobalResponse globalDashboard() {
        return service.dashboard();
    }
}
