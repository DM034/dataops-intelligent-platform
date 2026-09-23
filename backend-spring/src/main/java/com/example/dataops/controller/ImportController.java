package com.example.dataops.controller;

import com.example.dataops.dto.ImportDtos;
import com.example.dataops.service.ImportService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/import")
public class ImportController {
    private final ImportService service;

    public ImportController(ImportService service) {
        this.service = service;
    }

    @PostMapping("/sales")
    public ImportDtos.ImportResultResponse importSales(@RequestParam("file") MultipartFile file) {
        return service.importSales(file);
    }

    @PostMapping("/sales/jobs")
    public ImportDtos.ImportJobStartedResponse startSalesImport(@RequestParam("file") MultipartFile file, @RequestParam(value = "mode", required = false) String mode) {
        return service.startSalesImport(file, mode);
    }

    @PostMapping("/stocks")
    public ImportDtos.ImportResultResponse importStock(@RequestParam("file") MultipartFile file) {
        return service.importStock(file);
    }

    @PostMapping("/stocks/jobs")
    public ImportDtos.ImportJobStartedResponse startStockImport(@RequestParam("file") MultipartFile file, @RequestParam(value = "mode", required = false) String mode) {
        return service.startStockImport(file, mode);
    }

    @PostMapping("/auto/jobs")
    public ImportDtos.ImportJobStartedResponse startAutoImport(@RequestParam("file") MultipartFile file, @RequestParam(value = "mode", required = false) String mode) {
        return service.startAutoImport(file, mode);
    }

    @PostMapping("/preview")
    public ImportDtos.ImportPreviewResponse preview(@RequestParam("file") MultipartFile file) {
        return service.preview(file);
    }

    @GetMapping("/jobs")
    public List<ImportDtos.ImportJobSummaryResponse> jobs() {
        return service.jobs();
    }

    @GetMapping("/jobs/{jobId}")
    public ImportDtos.ImportJobProgressResponse jobProgress(@PathVariable String jobId) {
        return service.jobProgress(jobId);
    }

    @PostMapping("/jobs/{jobId}/cancel")
    public ImportDtos.ImportJobProgressResponse cancelJob(@PathVariable String jobId) {
        return service.cancelJob(jobId);
    }

    @GetMapping("/jobs/{jobId}/errors")
    public ResponseEntity<Resource> errorFile(@PathVariable String jobId) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + service.errorFileName(jobId) + "\"")
            .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
            .body(service.errorFile(jobId));
    }

    @GetMapping("/jobs/{jobId}/report")
    public ResponseEntity<Resource> reportFile(@PathVariable String jobId) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + service.reportFileName(jobId) + "\"")
            .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
            .body(service.reportFile(jobId));
    }

    @PostMapping("/cleanup")
    public Integer cleanup(@RequestParam(value = "days", defaultValue = "7") int days) {
        return service.cleanupOldFiles(days);
    }
}
