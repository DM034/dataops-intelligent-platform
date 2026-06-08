package com.example.dataops.controller;

import com.example.dataops.dto.DataGovernanceDtos;
import com.example.dataops.service.DataGovernanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/import-audit")
public class ImportAuditController {
    private final DataGovernanceService service;

    public ImportAuditController(DataGovernanceService service) {
        this.service = service;
    }

    @GetMapping
    public List<DataGovernanceDtos.ImportAuditResponse> audits() {
        return service.importAudits();
    }

    @GetMapping("/{id}")
    public DataGovernanceDtos.ImportAuditResponse audit(@PathVariable Long id) {
        return service.importAudit(id);
    }
}
