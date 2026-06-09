package com.example.dataops.controller;

import com.example.dataops.dto.AlerteDtos;
import com.example.dataops.service.AlerteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alertes")
public class AlerteController {
    private final AlerteService service;

    public AlerteController(AlerteService service) {
        this.service = service;
    }

    @GetMapping
    public AlerteDtos.AlerteListResponse alertes() {
        return service.findAll();
    }

    @GetMapping("/active")
    public AlerteDtos.AlerteListResponse activeAlertes() {
        return service.findActive();
    }

    @GetMapping("/{id}")
    public AlerteDtos.AlerteResponse alerte(@PathVariable Long id) {
        return service.findById(id);
    }

    @PatchMapping("/{id}/resolve")
    public AlerteDtos.AlerteResponse resolve(@PathVariable Long id, HttpServletRequest request) {
        return service.resolve(id, request);
    }

    @PatchMapping("/{id}/ignore")
    public AlerteDtos.AlerteResponse ignore(@PathVariable Long id, HttpServletRequest request) {
        return service.ignore(id, request);
    }

    @PostMapping("/generate")
    public AlerteDtos.AlerteGenerateResponse generate() {
        return service.generate();
    }
}
