package com.example.dataops.controller;

import com.example.dataops.dto.AgencyDtos;
import com.example.dataops.service.AgencyService;
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
@RequestMapping("/api/agencies")
public class AgencyController {
    private final AgencyService service;

    public AgencyController(AgencyService service) {
        this.service = service;
    }

    @GetMapping
    public List<AgencyDtos.AgencyResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AgencyDtos.AgencyResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgencyDtos.AgencyResponse create(@Valid @RequestBody AgencyDtos.AgencyRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public AgencyDtos.AgencyResponse update(@PathVariable Long id, @Valid @RequestBody AgencyDtos.AgencyRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

