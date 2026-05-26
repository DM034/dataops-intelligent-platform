package com.example.dataops.controller;

import com.example.dataops.dto.ImportDtos;
import com.example.dataops.service.ImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports")
public class ImportController {
    private final ImportService service;

    public ImportController(ImportService service) {
        this.service = service;
    }

    @PostMapping("/sales")
    public ImportDtos.ImportResultResponse importSales(@RequestParam("file") MultipartFile file) {
        return service.importSales(file);
    }

    @PostMapping("/stock")
    public ImportDtos.ImportResultResponse importStock(@RequestParam("file") MultipartFile file) {
        return service.importStock(file);
    }
}

