package com.example.dataops.service;

import com.example.dataops.dto.ImportDtos;
import com.example.dataops.dto.SaleDtos;
import com.example.dataops.dto.StockDtos;
import com.example.dataops.model.Agency;
import com.example.dataops.model.Product;
import com.example.dataops.model.StockMovementType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ImportService {
    private final SaleService saleService;
    private final StockService stockService;
    private final AgencyService agencyService;
    private final ProductService productService;
    private final BlockchainService blockchainService;

    public ImportService(SaleService saleService, StockService stockService, AgencyService agencyService, ProductService productService, BlockchainService blockchainService) {
        this.saleService = saleService;
        this.stockService = stockService;
        this.agencyService = agencyService;
        this.productService = productService;
        this.blockchainService = blockchainService;
    }

    @Transactional
    public ImportDtos.ImportResultResponse importSales(MultipartFile file) {
        int imported = 0;
        int skipped = 0;
        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
            .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            for (CSVRecord record : parser) {
                try {
                    Agency agency = agencyService.getByCode(record.get("agencyCode"));
                    Product product = productService.getBySku(record.get("sku"));
                    saleService.create(new SaleDtos.SaleRequest(
                        agency.getId(),
                        product.getId(),
                        Integer.parseInt(record.get("quantity")),
                        optionalDecimal(record, "unitPrice"),
                        LocalDate.parse(record.get("saleDate")),
                        optional(record, "reference")
                    ));
                    imported++;
                } catch (RuntimeException exception) {
                    skipped++;
                }
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to import sales CSV", exception);
        }
        blockchainService.append("SALES_CSV_IMPORTED", "system", "imported=" + imported + ",skipped=" + skipped);
        return new ImportDtos.ImportResultResponse(imported, skipped);
    }

    @Transactional
    public ImportDtos.ImportResultResponse importStock(MultipartFile file) {
        int imported = 0;
        int skipped = 0;
        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
            .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            for (CSVRecord record : parser) {
                try {
                    Agency agency = agencyService.getByCode(record.get("agencyCode"));
                    Product product = productService.getBySku(record.get("sku"));
                    stockService.create(new StockDtos.StockMovementRequest(
                        agency.getId(),
                        product.getId(),
                        StockMovementType.valueOf(record.get("type").toUpperCase()),
                        Integer.parseInt(record.get("quantity")),
                        optionalDateTime(record, "movementDate"),
                        optional(record, "reason")
                    ));
                    imported++;
                } catch (RuntimeException exception) {
                    skipped++;
                }
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to import stock CSV", exception);
        }
        blockchainService.append("STOCK_CSV_IMPORTED", "system", "imported=" + imported + ",skipped=" + skipped);
        return new ImportDtos.ImportResultResponse(imported, skipped);
    }

    private BigDecimal optionalDecimal(CSVRecord record, String name) {
        String value = optional(record, name);
        return value == null ? null : new BigDecimal(value);
    }

    private LocalDateTime optionalDateTime(CSVRecord record, String name) {
        String value = optional(record, name);
        return value == null ? null : LocalDateTime.parse(value);
    }

    private String optional(CSVRecord record, String name) {
        return record.isMapped(name) && !record.get(name).isBlank() ? record.get(name) : null;
    }
}

