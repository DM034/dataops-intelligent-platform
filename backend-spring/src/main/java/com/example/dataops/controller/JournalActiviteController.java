package com.example.dataops.controller;

import com.example.dataops.dto.JournalActiviteDtos;
import com.example.dataops.model.JournalNiveau;
import com.example.dataops.service.JournalActiviteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/journal-activite")
public class JournalActiviteController {
    private final JournalActiviteService service;

    public JournalActiviteController(JournalActiviteService service) {
        this.service = service;
    }

    @GetMapping
    public JournalActiviteDtos.JournalActivitePageResponse search(
        @RequestParam(required = false) JournalNiveau niveau,
        @RequestParam(required = false) String module,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateDebut,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFin,
        @RequestParam(required = false) String utilisateur,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return service.search(niveau, module, dateDebut, dateFin, utilisateur, page, size);
    }

    @GetMapping("/{id}")
    public JournalActiviteDtos.JournalActiviteResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
