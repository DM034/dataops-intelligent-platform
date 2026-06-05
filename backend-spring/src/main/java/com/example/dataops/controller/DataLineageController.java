package com.example.dataops.controller;

import com.example.dataops.dto.DataGovernanceDtos;
import com.example.dataops.service.DataGovernanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/data-lineage")
public class DataLineageController {
    private final DataGovernanceService service;

    public DataLineageController(DataGovernanceService service) {
        this.service = service;
    }

    @GetMapping
    public List<DataGovernanceDtos.DataLineageResponse> lineage() {
        return service.lineage();
    }

    @GetMapping("/{id}")
    public DataGovernanceDtos.DataLineageResponse lineage(@PathVariable Long id) {
        return service.lineage(id);
    }
}
