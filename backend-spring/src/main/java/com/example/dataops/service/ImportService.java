package com.example.dataops.service;

import com.example.dataops.dto.ImportDtos;
import com.example.dataops.dto.SaleDtos;
import com.example.dataops.dto.StockDtos;
import com.example.dataops.exception.ResourceNotFoundException;
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ImportService {
    private static final ProgressSink NO_PROGRESS = processedRows -> {
    };

    private final SaleService saleService;
    private final StockService stockService;
    private final AgencyService agencyService;
    private final ProductService productService;
    private final BlockchainService blockchainService;
    private final DataGovernanceService dataGovernanceService;
    private final Map<String, ImportJob> jobs = new ConcurrentHashMap<>();

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
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return importSales(sourceName(file), reader, currentUserId(), NO_PROGRESS);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read sales CSV: " + exception.getMessage());
        }
    }

    public ImportDtos.ImportJobStartedResponse startSalesImport(MultipartFile file) {
        return startImport(file, "SALES");
    }

    @Transactional
    public ImportDtos.ImportResultResponse importStock(MultipartFile file) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return importStock(sourceName(file), reader, currentUserId(), NO_PROGRESS);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read stock CSV: " + exception.getMessage());
        }
    }

    public ImportDtos.ImportJobStartedResponse startStockImport(MultipartFile file) {
        return startImport(file, "STOCKS");
    }

    public ImportDtos.ImportJobProgressResponse jobProgress(String jobId) {
        ImportJob job = jobs.get(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("Import job not found: " + jobId);
        }
        return job.toResponse();
    }

    private ImportDtos.ImportJobStartedResponse startImport(MultipartFile file, String type) {
        String jobId = UUID.randomUUID().toString();
        String userId = currentUserId();
        String originalName = sourceName(file);
        try {
            Path tempFile = Files.createTempFile("dataops-import-" + type.toLowerCase() + "-", ".csv");
            file.transferTo(tempFile);
            ImportJob job = new ImportJob(jobId, type, originalName);
            jobs.put(jobId, job);

            CompletableFuture.runAsync(() -> {
                try {
                    job.counting();
                    job.totalRows(countDataRows(tempFile));
                    ImportDtos.ImportResultResponse result;
                    if ("SALES".equals(type)) {
                        try (Reader reader = Files.newBufferedReader(tempFile, StandardCharsets.UTF_8)) {
                            result = importSales(originalName, reader, userId, job::processed);
                        }
                    } else {
                        try (Reader reader = Files.newBufferedReader(tempFile, StandardCharsets.UTF_8)) {
                            result = importStock(originalName, reader, userId, job::processed);
                        }
                    }
                    job.completed(result);
                } catch (Exception exception) {
                    job.failed(exception.getMessage());
                } finally {
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (IOException ignored) {
                    }
                }
            });

            return new ImportDtos.ImportJobStartedResponse(jobId, "STARTING", 0, 0);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to start " + type.toLowerCase() + " import: " + exception.getMessage());
        }
    }

    private long countDataRows(Path path) throws IOException {
        try (var lines = Files.lines(path, StandardCharsets.UTF_8)) {
            return Math.max(0, lines.count() - 1);
        }
    }

    private ImportDtos.ImportResultResponse importSales(String sourceName, Reader reader, String userId, ProgressSink progressSink) {
        int imported = 0;
        int skipped = 0;
        List<ImportDtos.ImportLineError> errors = new ArrayList<>();
        QualityTracker quality = new QualityTracker();
        String importFileId = importFileId("sales", sourceName);
        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            validateHeaders(parser, Set.of("date", "agencyCode", "productCode", "quantity", "unitPrice"));
            for (CSVRecord record : parser) {
                try {
                    if (isEmpty(record)) {
                        skipped++;
                        continue;
                    }
                    quality.row();
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
                    blockchainService.addBlock("IMPORT_SALE", "SALE", sale.id(), userId, record.toString());
                    imported++;
                } catch (RuntimeException exception) {
                    skipped++;
                    errors.add(new ImportDtos.ImportLineError(record.getRecordNumber(), exception.getMessage()));
                } finally {
                    progressSink.processed(record.getRecordNumber() - 1);
                }
            }
        } catch (Exception exception) {
            skipped++;
            errors.add(new ImportDtos.ImportLineError(0, "Unable to import sales CSV: " + exception.getMessage()));
        }
        DataGovernanceService.ImportGovernanceResult governance = dataGovernanceService.recordImport(
            importFileId,
            sourceName,
            "CSV_SALES",
            "CSV_TO_SALE_ENTITIES",
            quality.toMetrics(imported, errors.size()),
            userId
        );
        return new ImportDtos.ImportResultResponse(imported, skipped, errors, governance.reportId(), governance.lineageId());
    }

    private ImportDtos.ImportResultResponse importStock(String sourceName, Reader reader, String userId, ProgressSink progressSink) {
        int imported = 0;
        int skipped = 0;
        List<ImportDtos.ImportLineError> errors = new ArrayList<>();
        QualityTracker quality = new QualityTracker();
        String importFileId = importFileId("stocks", sourceName);
        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            validateHeaders(parser, Set.of("date", "agencyCode", "productCode", "quantity", "type"));
            for (CSVRecord record : parser) {
                try {
                    if (isEmpty(record)) {
                        skipped++;
                        continue;
                    }
                    quality.row();
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
                    blockchainService.addBlock("IMPORT_STOCK", "STOCK", movement.id(), userId, record.toString());
                    imported++;
                } catch (RuntimeException exception) {
                    skipped++;
                    errors.add(new ImportDtos.ImportLineError(record.getRecordNumber(), exception.getMessage()));
                } finally {
                    progressSink.processed(record.getRecordNumber() - 1);
                }
            }
        } catch (Exception exception) {
            skipped++;
            errors.add(new ImportDtos.ImportLineError(0, "Unable to import stock CSV: " + exception.getMessage()));
        }
        DataGovernanceService.ImportGovernanceResult governance = dataGovernanceService.recordImport(
            importFileId,
            sourceName,
            "CSV_STOCK",
            "CSV_TO_STOCK_MOVEMENTS",
            quality.toMetrics(imported, errors.size()),
            userId
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
            LocalDate date = LocalDate.parse(value(record, column));
            quality.validFormat();
            return date;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid date format for " + column + ", expected yyyy-MM-dd");
        }
    }

    private Integer parseInteger(CSVRecord record, String column, QualityTracker quality) {
        try {
            Integer number = Integer.parseInt(value(record, column));
            quality.validFormat();
            return number;
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

    private String importFileId(String prefix, String sourceName) {
        return prefix + "-" + sourceName.replaceAll("[^a-zA-Z0-9._-]", "_") + "-" + Instant.now().toEpochMilli();
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

    @FunctionalInterface
    private interface ProgressSink {
        void processed(long processedRows);
    }

    private static final class ImportJob {
        private final String id;
        private final String type;
        private final String fileName;
        private long totalRows;
        private final Instant startedAt = Instant.now();
        private long processedRows;
        private int importedRows;
        private int skippedRows;
        private String status = "RUNNING";
        private String message = "Import en cours";
        private ImportDtos.ImportResultResponse result;
        private Instant finishedAt;

        private ImportJob(String id, String type, String fileName) {
            this.id = id;
            this.type = type;
            this.fileName = fileName;
        }

        private synchronized void counting() {
            this.status = "COUNTING";
            this.message = "Comptage des lignes du fichier...";
        }

        private synchronized void totalRows(long totalRows) {
            this.totalRows = totalRows;
            this.status = "RUNNING";
            this.message = "Traitement " + type + " : 0/" + totalRows + " lignes";
        }

        private synchronized void processed(long processedRows) {
            this.processedRows = Math.max(this.processedRows, processedRows);
            this.message = "Traitement " + type + " : " + this.processedRows + "/" + totalRows + " lignes";
        }

        private synchronized void completed(ImportDtos.ImportResultResponse result) {
            this.result = result;
            this.importedRows = result.importedRows();
            this.skippedRows = result.skippedRows();
            this.processedRows = Math.max(processedRows, totalRows);
            this.status = "COMPLETED";
            this.message = fileName + " importé";
            this.finishedAt = Instant.now();
        }

        private synchronized void failed(String message) {
            this.status = "FAILED";
            this.message = message == null || message.isBlank() ? "Import échoué" : message;
            this.finishedAt = Instant.now();
        }

        private synchronized ImportDtos.ImportJobProgressResponse toResponse() {
            int progress = totalRows == 0 ? ("COMPLETED".equals(status) ? 100 : 0) : (int) Math.min(100, Math.round((processedRows * 100.0) / totalRows));
            if ("COMPLETED".equals(status)) {
                progress = 100;
            }
            return new ImportDtos.ImportJobProgressResponse(id, status, progress, totalRows, processedRows, importedRows, skippedRows, message, result, startedAt, finishedAt);
        }
    }

    private static final class QualityTracker {
        private final Set<String> seenRows = new HashSet<>();
        private int totalRows;
        private int completeRows;
        private int validFormatRows;
        private int uniqueRows;
        private int duplicateRows;
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
                duplicateRows++;
                return false;
            }
            uniqueRows++;
            return true;
        }

        void consistent() {
            consistentRows++;
        }

        DataGovernanceService.QualityMetrics toMetrics(int validRows, int errorRows) {
            return new DataGovernanceService.QualityMetrics(totalRows, validRows, errorRows, duplicateRows, completeRows, validFormatRows, uniqueRows, consistentRows);
        }
    }
}
