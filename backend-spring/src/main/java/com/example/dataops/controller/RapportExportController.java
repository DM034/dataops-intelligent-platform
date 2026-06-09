package com.example.dataops.controller;

import com.example.dataops.dto.RapportExportDtos;
import com.example.dataops.model.HistoriqueModule;
import com.example.dataops.model.JournalNiveau;
import com.example.dataops.service.JournalActiviteService;
import com.example.dataops.service.RapportExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/rapports/export")
public class RapportExportController {
    private final RapportExportService service;
    private final JournalActiviteService journalActiviteService;

    public RapportExportController(RapportExportService service, JournalActiviteService journalActiviteService) {
        this.service = service;
        this.journalActiviteService = journalActiviteService;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(
        @RequestParam RapportExportDtos.TypeRapport typeRapport,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
        @RequestParam(required = false) HistoriqueModule module,
        @RequestParam(required = false) String statut
    ) {
        RapportExportService.ExportedReport report = service.exportPdf(new RapportExportDtos.RapportExportFilter(typeRapport, dateDebut, dateFin, module, statut));
        journalActiviteService.journaliser(JournalNiveau.INFO, "EXPORT_RAPPORT", "RAPPORTS", "Export PDF d'un rapport", "typeRapport=" + typeRapport, report.fileName());
        return response(report);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel(
        @RequestParam RapportExportDtos.TypeRapport typeRapport,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
        @RequestParam(required = false) HistoriqueModule module,
        @RequestParam(required = false) String statut
    ) {
        RapportExportService.ExportedReport report = service.exportExcel(new RapportExportDtos.RapportExportFilter(typeRapport, dateDebut, dateFin, module, statut));
        journalActiviteService.journaliser(JournalNiveau.INFO, "EXPORT_RAPPORT", "RAPPORTS", "Export Excel d'un rapport", "typeRapport=" + typeRapport, report.fileName());
        return response(report);
    }

    private ResponseEntity<byte[]> response(RapportExportService.ExportedReport report) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(report.fileName()).build().toString())
            .header(HttpHeaders.CONTENT_TYPE, report.contentType())
            .body(report.content());
    }
}
