package com.example.dataops.controller;

import com.example.dataops.dto.AlertDtos;
import com.example.dataops.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping
    public List<AlertDtos.AlertResponse> findAll(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return service.findAll(activeOnly);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertDtos.AlertResponse create(@Valid @RequestBody AlertDtos.AlertRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{id}/resolve")
    public AlertDtos.AlertResponse resolve(@PathVariable Long id) {
        return service.resolve(id);
    }
}

