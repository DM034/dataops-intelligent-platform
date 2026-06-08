package com.example.dataops.controller;

import com.example.dataops.dto.RecommendationDtos;
import com.example.dataops.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecommendationDtos.RecommendationResponse> recommendations() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public RecommendationDtos.RecommendationResponse recommendation(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}/status")
    public RecommendationDtos.RecommendationResponse updateStatus(@PathVariable Long id, @Valid @RequestBody RecommendationDtos.RecommendationStatusRequest request) {
        return service.updateStatus(id, request.status());
    }

    @PostMapping("/generate")
    public RecommendationDtos.RecommendationGenerateResponse generate() {
        return service.generate();
    }
}
