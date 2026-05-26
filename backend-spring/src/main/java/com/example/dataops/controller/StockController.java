package com.example.dataops.controller;

import com.example.dataops.dto.StockDtos;
import com.example.dataops.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
public class StockController {
    private final StockService service;

    public StockController(StockService service) {
        this.service = service;
    }

    @GetMapping("/movements")
    public List<StockDtos.StockMovementResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/movements/{id}")
    public StockDtos.StockMovementResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public StockDtos.StockMovementResponse create(@Valid @RequestBody StockDtos.StockMovementRequest request) {
        return service.create(request);
    }

    @PutMapping("/movements/{id}")
    public StockDtos.StockMovementResponse update(@PathVariable Long id, @Valid @RequestBody StockDtos.StockMovementRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/movements/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/levels")
    public List<StockDtos.StockLevelResponse> stockLevels() {
        return service.stockLevels();
    }
}
