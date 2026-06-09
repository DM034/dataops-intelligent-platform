package com.example.dataops.controller;

import com.example.dataops.dto.RegleMetierDtos;
import com.example.dataops.model.RegleMetierModule;
import com.example.dataops.service.RegleMetierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/regles-metier")
public class RegleMetierController {
    private final RegleMetierService service;

    public RegleMetierController(RegleMetierService service) {
        this.service = service;
    }

    @GetMapping
    public List<RegleMetierDtos.RegleMetierResponse> findAll(@RequestParam(required = false) RegleMetierModule module) {
        return service.findAll(module);
    }

    @GetMapping("/{code}")
    public RegleMetierDtos.RegleMetierResponse findByCode(@PathVariable String code) {
        return service.findByCode(code);
    }

    @PutMapping("/{code}")
    public RegleMetierDtos.RegleMetierResponse update(@PathVariable String code, @Valid @RequestBody RegleMetierDtos.UpdateRegleMetierRequest request) {
        return service.update(code, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegleMetierDtos.RegleMetierResponse create(@Valid @RequestBody RegleMetierDtos.CreateRegleMetierRequest request) {
        return service.create(request);
    }
}
