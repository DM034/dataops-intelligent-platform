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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        List<ImportDtos.ImportLineError> errors = new ArrayList<>();
        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
            .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            validateHeaders(parser, Set.of("date", "agencyCode", "productCode", "quantity", "unitPrice"));
            for (CSVRecord record : parser) {
                if (isEmpty(record)) {
                    skipped++;
                    continue;
                }
                try {
                    validateRequired(record, "date", "agencyCode", "productCode", "quantity", "unitPrice");
                    Agency agency = agencyService.getByCode(value(record, "agencyCode"));
                    Product product = productService.getBySku(value(record, "productCode"));
                    SaleDtos.SaleResponse sale = saleService.create(new SaleDtos.SaleRequest(
                        agency.getId(),
                        product.getId(),
                        Integer.parseInt(value(record, "quantity")),
                        new BigDecimal(value(record, "unitPrice")),
                        LocalDate.parse(value(record, "date")),
                        "CSV_IMPORT_LINE_" + record.getRecordNumber()
                    ));
                    blockchainService.addBlock("IMPORT_SALE", "SALE", sale.id(), currentUserId(), record.toString());
                    imported++;
                } catch (RuntimeException exception) {
                    skipped++;
                    errors.add(new ImportDtos.ImportLineError(record.getRecordNumber(), exception.getMessage()));
                }
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to import sales CSV", exception);
        }
        return new ImportDtos.ImportResultResponse(imported, skipped, errors);
    }

    @Transactional
    public ImportDtos.ImportResultResponse importStock(MultipartFile file) {
        int imported = 0;
        int skipped = 0;
        List<ImportDtos.ImportLineError> errors = new ArrayList<>();
        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
            .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            validateHeaders(parser, Set.of("date", "agencyCode", "productCode", "quantity", "type"));
            for (CSVRecord record : parser) {
                if (isEmpty(record)) {
                    skipped++;
                    continue;
                }
                try {
                    validateRequired(record, "date", "agencyCode", "productCode", "quantity", "type");
                    Agency agency = agencyService.getByCode(value(record, "agencyCode"));
                    Product product = productService.getBySku(value(record, "productCode"));
                    StockDtos.StockMovementResponse movement = stockService.create(new StockDtos.StockMovementRequest(
                        agency.getId(),
                        product.getId(),
                        StockMovementType.valueOf(value(record, "type").toUpperCase()),
                        Integer.parseInt(value(record, "quantity")),
                        LocalDateTime.of(LocalDate.parse(value(record, "date")), LocalTime.MIDNIGHT),
                        "CSV_IMPORT_LINE_" + record.getRecordNumber()
                    ));
                    blockchainService.addBlock("IMPORT_STOCK", "STOCK", movement.id(), currentUserId(), record.toString());
                    imported++;
                } catch (RuntimeException exception) {
                    skipped++;
                    errors.add(new ImportDtos.ImportLineError(record.getRecordNumber(), exception.getMessage()));
                }
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to import stock CSV", exception);
        }
        return new ImportDtos.ImportResultResponse(imported, skipped, errors);
    }

    private void validateHeaders(CSVParser parser, Set<String> requiredColumns) {
        Set<String> actualColumns = parser.getHeaderMap().keySet();
        List<String> missingColumns = requiredColumns.stream()
            .filter(column -> !actualColumns.contains(column))
            .toList();
        if (!missingColumns.isEmpty()) {
            throw new IllegalArgumentException("Missing required columns: " + String.join(", ", missingColumns));
        }
    }

    private void validateRequired(CSVRecord record, String... columns) {
        for (String column : columns) {
            if (value(record, column).isBlank()) {
                throw new IllegalArgumentException("Missing required value: " + column);
            }
        }
    }

    private boolean isEmpty(CSVRecord record) {
        for (String value : record) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String value(CSVRecord record, String name) {
        return record.get(name).trim();
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
