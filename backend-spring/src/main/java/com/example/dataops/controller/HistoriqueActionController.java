package com.example.dataops.controller;

import com.example.dataops.dto.HistoriqueActionDtos;
import com.example.dataops.model.HistoriqueModule;
import com.example.dataops.service.HistoriqueActionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/historique")
public class HistoriqueActionController {
    private final HistoriqueActionService service;

    public HistoriqueActionController(HistoriqueActionService service) {
        this.service = service;
    }

    @GetMapping
    public List<HistoriqueActionDtos.HistoriqueActionResponse> historique() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public HistoriqueActionDtos.HistoriqueActionResponse historique(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/search")
    public List<HistoriqueActionDtos.HistoriqueActionResponse> search(
        @RequestParam(required = false) HistoriqueModule module,
        @RequestParam(required = false) String utilisateur,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateDebut,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFin,
        @RequestParam(required = false) String action
    ) {
        return service.search(module, utilisateur, dateDebut, dateFin, action);
    }
}
