package com.example.dataops.controller;

import com.example.dataops.dto.DataGovernanceDtos;
import com.example.dataops.service.DataGovernanceService;
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
@RequestMapping("/api/catalog")
public class DataCatalogController {
    private final DataGovernanceService service;

    public DataCatalogController(DataGovernanceService service) {
        this.service = service;
    }

    @GetMapping
    public List<DataGovernanceDtos.DataCatalogResponse> catalog() {
        return service.catalog();
    }

    @GetMapping("/{id}")
    public DataGovernanceDtos.DataCatalogResponse catalog(@PathVariable Long id) {
        return service.catalog(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataGovernanceDtos.DataCatalogResponse create(@Valid @RequestBody DataGovernanceDtos.DataCatalogRequest request) {
        return service.createCatalog(request);
    }

    @PutMapping("/{id}")
    public DataGovernanceDtos.DataCatalogResponse update(@PathVariable Long id, @Valid @RequestBody DataGovernanceDtos.DataCatalogRequest request) {
        return service.updateCatalog(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteCatalog(id);
    }
}
