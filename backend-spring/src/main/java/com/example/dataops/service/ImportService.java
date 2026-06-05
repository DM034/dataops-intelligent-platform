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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ImportService {
    private final SaleService saleService;
    private final StockService stockService;
    private final AgencyService agencyService;
    private final ProductService productService;
    private final BlockchainService blockchainService;
    private final DataGovernanceService dataGovernanceService;

    public ImportService(SaleService saleService, StockService stockService, AgencyService agencyService, ProductService productService, BlockchainService blockchainService, DataGovernanceService dataGovernanceService) {
        this.saleService = saleService;
        this.stockService = stockService;
        this.agencyService = agencyService;
        this.productService = productService;
        this.blockchainService = blockchainService;
        this.dataGovernanceService = dataGovernanceService;
    }

    @Transactional
    public ImportDtos.ImportResultResponse importSales(MultipartFile file) {
        int imported = 0;
        int skipped = 0;
        List<ImportDtos.ImportLineError> errors = new ArrayList<>();
        QualityTracker quality = new QualityTracker();
        String importFileId = importFileId("sales", file);
        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
            .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            validateHeaders(parser, Set.of("date", "agencyCode", "productCode", "quantity", "unitPrice"));
            for (CSVRecord record : parser) {
                if (isEmpty(record)) {
                    skipped++;
                    continue;
                }
                quality.row();
                try {
                    validateRequired(record, quality, "date", "agencyCode", "productCode", "quantity", "unitPrice");
                    if (!quality.unique(uniqueKey(record, "date", "agencyCode", "productCode", "quantity", "unitPrice"))) {
                        throw new IllegalArgumentException("Duplicate sale import line");
                    }
                    LocalDate saleDate = parseDate(record, "date", quality);
                    Integer quantity = parseInteger(record, "quantity", quality);
                    BigDecimal unitPrice = parseDecimal(record, "unitPrice", quality);
                    Agency agency = agencyService.getByCode(value(record, "agencyCode"));
                    Product product = productService.getBySku(value(record, "productCode"));
                    validateSalesConsistency(quantity, unitPrice, quality);
                    SaleDtos.SaleResponse sale = saleService.create(new SaleDtos.SaleRequest(
                        agency.getId(),
                        product.getId(),
                        quantity,
                        unitPrice,
                        saleDate,
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
            skipped++;
            errors.add(new ImportDtos.ImportLineError(0, "Unable to import sales CSV: " + exception.getMessage()));
        }
        DataGovernanceService.ImportGovernanceResult governance = dataGovernanceService.recordImport(
            importFileId,
            sourceName(file),
            "CSV_SALES",
            "CSV_TO_SALE_ENTITIES",
            quality.toMetrics(imported, errors.size())
        );
        return new ImportDtos.ImportResultResponse(imported, skipped, errors, governance.reportId(), governance.lineageId());
    }

    @Transactional
    public ImportDtos.ImportResultResponse importStock(MultipartFile file) {
        int imported = 0;
        int skipped = 0;
        List<ImportDtos.ImportLineError> errors = new ArrayList<>();
        QualityTracker quality = new QualityTracker();
        String importFileId = importFileId("stocks", file);
        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
            .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            validateHeaders(parser, Set.of("date", "agencyCode", "productCode", "quantity", "type"));
            for (CSVRecord record : parser) {
                if (isEmpty(record)) {
                    skipped++;
                    continue;
                }
                quality.row();
                try {
                    validateRequired(record, quality, "date", "agencyCode", "productCode", "quantity", "type");
                    if (!quality.unique(uniqueKey(record, "date", "agencyCode", "productCode", "quantity", "type"))) {
                        throw new IllegalArgumentException("Duplicate stock import line");
                    }
                    LocalDate movementDay = parseDate(record, "date", quality);
                    Integer quantity = parseInteger(record, "quantity", quality);
                    StockMovementType type = parseStockMovementType(record, quality);
                    Agency agency = agencyService.getByCode(value(record, "agencyCode"));
                    Product product = productService.getBySku(value(record, "productCode"));
                    validateStockConsistency(quantity, quality);
                    StockDtos.StockMovementResponse movement = stockService.create(new StockDtos.StockMovementRequest(
                        agency.getId(),
                        product.getId(),
                        type,
                        quantity,
                        LocalDateTime.of(movementDay, LocalTime.MIDNIGHT),
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
            skipped++;
            errors.add(new ImportDtos.ImportLineError(0, "Unable to import stock CSV: " + exception.getMessage()));
        }
        DataGovernanceService.ImportGovernanceResult governance = dataGovernanceService.recordImport(
            importFileId,
            sourceName(file),
            "CSV_STOCK",
            "CSV_TO_STOCK_MOVEMENTS",
            quality.toMetrics(imported, errors.size())
        );
        return new ImportDtos.ImportResultResponse(imported, skipped, errors, governance.reportId(), governance.lineageId());
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

    private void validateRequired(CSVRecord record, QualityTracker quality, String... columns) {
        List<String> missingColumns = new ArrayList<>();
        for (String column : columns) {
            if (value(record, column).isBlank()) {
                missingColumns.add(column);
            }
        }
        if (missingColumns.isEmpty()) {
            quality.complete();
            return;
        }
        throw new IllegalArgumentException("Missing required value(s): " + String.join(", ", missingColumns));
    }

    private LocalDate parseDate(CSVRecord record, String column, QualityTracker quality) {
        try {
            return LocalDate.parse(value(record, column));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid date format for " + column + ", expected yyyy-MM-dd");
        }
    }

    private Integer parseInteger(CSVRecord record, String column, QualityTracker quality) {
        try {
            return Integer.parseInt(value(record, column));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid integer format for " + column);
        }
    }

    private BigDecimal parseDecimal(CSVRecord record, String column, QualityTracker quality) {
        try {
            quality.validFormat();
            return new BigDecimal(value(record, column));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid decimal format for " + column);
        }
    }

    private StockMovementType parseStockMovementType(CSVRecord record, QualityTracker quality) {
        try {
            StockMovementType type = StockMovementType.valueOf(value(record, "type").toUpperCase());
            quality.validFormat();
            return type;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid stock movement type, expected IN, OUT or ADJUSTMENT");
        }
    }

    private void validateSalesConsistency(Integer quantity, BigDecimal unitPrice, QualityTracker quality) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Inconsistent quantity, expected a positive value");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Inconsistent unitPrice, expected a positive or zero value");
        }
        if (quantity > 10000 || unitPrice.compareTo(BigDecimal.valueOf(100000)) > 0) {
            throw new IllegalArgumentException("Outlier sale value detected");
        }
        quality.consistent();
    }

    private void validateStockConsistency(Integer quantity, QualityTracker quality) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Inconsistent quantity, expected a positive value");
        }
        if (quantity > 100000) {
            throw new IllegalArgumentException("Outlier stock quantity detected");
        }
        quality.consistent();
    }

    private String uniqueKey(CSVRecord record, String... columns) {
        List<String> values = new ArrayList<>();
        for (String column : columns) {
            values.add(value(record, column));
        }
        return String.join("|", values);
    }

    private String importFileId(String prefix, MultipartFile file) {
        return prefix + "-" + sourceName(file).replaceAll("[^a-zA-Z0-9._-]", "_") + "-" + Instant.now().toEpochMilli();
    }

    private String sourceName(MultipartFile file) {
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            return "import.csv";
        }
        return file.getOriginalFilename();
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

    private static final class QualityTracker {
        private final Set<String> seenRows = new HashSet<>();
        private int totalRows;
        private int completeRows;
        private int validFormatRows;
        private int uniqueRows;
        private int consistentRows;

        void row() {
            totalRows++;
        }

        void complete() {
            completeRows++;
        }

        void validFormat() {
            validFormatRows++;
        }

        boolean unique(String key) {
            if (!seenRows.add(key)) {
                return false;
            }
            uniqueRows++;
            return true;
        }

        void consistent() {
            consistentRows++;
        }

        DataGovernanceService.QualityMetrics toMetrics(int validRows, int errorRows) {
            return new DataGovernanceService.QualityMetrics(totalRows, validRows, errorRows, completeRows, validFormatRows, uniqueRows, consistentRows);
        }
    }
}
