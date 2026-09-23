package com.example.dataops.service;

import com.example.dataops.dto.ImportDtos;
import com.example.dataops.exception.BusinessException;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.model.Agency;
import com.example.dataops.model.ImportJob;
import com.example.dataops.model.Product;
import com.example.dataops.model.Sale;
import com.example.dataops.model.StockMovement;
import com.example.dataops.model.StockMovementType;
import com.example.dataops.repository.ImportJobRepository;
import com.example.dataops.repository.SaleRepository;
import com.example.dataops.repository.StockMovementRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ImportService {
    private static final int BATCH_SIZE = 1_000;
    private static final int MAX_ERRORS_IN_RESPONSE = 50;
    private static final int JOB_UPDATE_INTERVAL = 500;
    private static final Set<String> SALES_COLUMNS = Set.of("date", "agencyCode", "productCode", "quantity", "unitPrice");
    private static final Set<String> STOCK_COLUMNS = Set.of("date", "agencyCode", "productCode", "quantity", "type");

    private final SaleRepository saleRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ImportJobRepository importJobRepository;
    private final AgencyService agencyService;
    private final ProductService productService;
    private final BlockchainService blockchainService;
    private final DataGovernanceService dataGovernanceService;
    private final ConcurrentMap<String, Boolean> cancelRequests = new ConcurrentHashMap<>();

    public ImportService(
        SaleRepository saleRepository,
        StockMovementRepository stockMovementRepository,
        ImportJobRepository importJobRepository,
        AgencyService agencyService,
        ProductService productService,
        BlockchainService blockchainService,
        DataGovernanceService dataGovernanceService
    ) {
        this.saleRepository = saleRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.importJobRepository = importJobRepository;
        this.agencyService = agencyService;
        this.productService = productService;
        this.blockchainService = blockchainService;
        this.dataGovernanceService = dataGovernanceService;
    }

    public ImportDtos.ImportResultResponse importSales(MultipartFile file) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return importSales(sourceName(file), reader, currentUserId(), null, null, "PARTIAL_IMPORT");
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read sales CSV: " + exception.getMessage());
        }
    }

    public ImportDtos.ImportResultResponse importStock(MultipartFile file) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return importStock(sourceName(file), reader, currentUserId(), null, null, "PARTIAL_IMPORT");
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read stock CSV: " + exception.getMessage());
        }
    }

    public ImportDtos.ImportJobStartedResponse startSalesImport(MultipartFile file) {
        return startImport(file, "SALES", "PARTIAL_IMPORT");
    }

    public ImportDtos.ImportJobStartedResponse startStockImport(MultipartFile file) {
        return startImport(file, "STOCKS", "PARTIAL_IMPORT");
    }

    public ImportDtos.ImportJobStartedResponse startSalesImport(MultipartFile file, String mode) {
        return startImport(file, "SALES", normalizeMode(mode));
    }

    public ImportDtos.ImportJobStartedResponse startStockImport(MultipartFile file, String mode) {
        return startImport(file, "STOCKS", normalizeMode(mode));
    }

    public ImportDtos.ImportJobStartedResponse startAutoImport(MultipartFile file, String mode) {
        ImportDtos.ImportPreviewResponse preview = preview(file);
        if (!"VALID".equals(preview.status())) {
            throw new BusinessException("Unable to detect a valid import type: " + preview.message());
        }
        return startImport(file, preview.detectedType(), normalizeMode(mode));
    }

    public ImportDtos.ImportPreviewResponse preview(MultipartFile file) {
        String detectedType = "UNKNOWN";
        List<String> headers = List.of();
        List<String> missingColumns = List.of();
        List<ImportDtos.ImportLineError> sampleErrors = new ArrayList<>();
        try {
            Path tempFile = Files.createTempFile("dataops-import-preview-", ".csv");
            file.transferTo(tempFile);
            long totalRows = countDataRows(tempFile);
            try (Reader reader = Files.newBufferedReader(tempFile, StandardCharsets.UTF_8);
                 CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
                headers = new ArrayList<>(parser.getHeaderMap().keySet());
                Set<String> actual = parser.getHeaderMap().keySet();
                if (actual.containsAll(SALES_COLUMNS)) {
                    detectedType = "SALES";
                    missingColumns = List.of();
                } else if (actual.containsAll(STOCK_COLUMNS)) {
                    detectedType = "STOCKS";
                    missingColumns = List.of();
                } else {
                    List<String> salesMissing = SALES_COLUMNS.stream().filter(column -> !actual.contains(column)).toList();
                    List<String> stockMissing = STOCK_COLUMNS.stream().filter(column -> !actual.contains(column)).toList();
                    missingColumns = salesMissing.size() <= stockMissing.size() ? salesMissing : stockMissing;
                }
                int checked = 0;
                for (CSVRecord record : parser) {
                    if (checked >= 20) {
                        break;
                    }
                    checked++;
                    if (isEmpty(record)) {
                        continue;
                    }
                    try {
                        if ("SALES".equals(detectedType)) {
                            validatePreviewSales(record);
                        } else if ("STOCKS".equals(detectedType)) {
                            validatePreviewStock(record);
                        }
                    } catch (RuntimeException exception) {
                        sampleErrors.add(new ImportDtos.ImportLineError(record.getRecordNumber(), exception.getMessage()));
                    }
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
            String status = "UNKNOWN".equals(detectedType) || !missingColumns.isEmpty() ? "INVALID" : (sampleErrors.isEmpty() ? "VALID" : "VALID_WITH_WARNINGS");
            String message = "UNKNOWN".equals(detectedType) ? "Colonnes non reconnues" : "Type détecté : " + detectedType;
            return new ImportDtos.ImportPreviewResponse(detectedType, status, totalRows, headers, missingColumns, sampleErrors, message);
        } catch (IOException exception) {
            return new ImportDtos.ImportPreviewResponse(detectedType, "INVALID", 0, headers, missingColumns, sampleErrors, exception.getMessage());
        }
    }

    public List<ImportDtos.ImportJobSummaryResponse> jobs() {
        return importJobRepository.findAllByOrderByStartedAtDesc().stream()
            .map(this::toSummaryResponse)
            .toList();
    }

    public ImportDtos.ImportJobProgressResponse jobProgress(String jobId) {
        ImportJob job = jobEntity(jobId);
        ImportDtos.ImportResultResponse result = null;
        if ("COMPLETED".equals(job.getStatus()) || "FAILED".equals(job.getStatus()) || "CANCELLED".equals(job.getStatus())) {
            result = new ImportDtos.ImportResultResponse(job.getImportedRows(), job.getSkippedRows(), readErrorPreview(job), job.getDataQualityReportId(), job.getDataLineageId());
        }
        return toProgressResponse(job, result);
    }

    public ImportDtos.ImportJobProgressResponse cancelJob(String jobId) {
        ImportJob job = jobEntity(jobId);
        if ("COMPLETED".equals(job.getStatus()) || "FAILED".equals(job.getStatus()) || "CANCELLED".equals(job.getStatus())) {
            return toProgressResponse(job, null);
        }
        cancelRequests.put(jobId, true);
        job.setStatus("CANCELLING");
        job.setMessage("Annulation demandée...");
        importJobRepository.save(job);
        return toProgressResponse(job, null);
    }

    public Resource errorFile(String jobId) {
        ImportJob job = jobEntity(jobId);
        if (job.getErrorFilePath() == null || job.getErrorFilePath().isBlank()) {
            throw new ResourceNotFoundException("No error file for import job: " + jobId);
        }
        Path path = Path.of(job.getErrorFilePath());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Error file not found for import job: " + jobId);
        }
        return new FileSystemResource(path);
    }

    public String errorFileName(String jobId) {
        ImportJob job = jobEntity(jobId);
        return "import-errors-" + safeFileName(job.getFileName()) + "-" + jobId + ".csv";
    }

    public Resource reportFile(String jobId) {
        ImportJob job = jobEntity(jobId);
        try {
            Path report = Files.createTempFile("dataops-import-report-" + jobId + "-", ".csv");
            try (BufferedWriter writer = Files.newBufferedWriter(report, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader("champ", "valeur").build())) {
                printer.printRecord("jobId", job.getId());
                printer.printRecord("fileName", job.getFileName());
                printer.printRecord("type", job.getType());
                printer.printRecord("mode", job.getMode());
                printer.printRecord("status", job.getStatus());
                printer.printRecord("totalRows", job.getTotalRows());
                printer.printRecord("processedRows", job.getProcessedRows());
                printer.printRecord("importedRows", job.getImportedRows());
                printer.printRecord("skippedRows", job.getSkippedRows());
                printer.printRecord("errorCount", job.getErrorCount());
                printer.printRecord("rowsPerSecond", rowsPerSecond(job));
                printer.printRecord("qualityReportId", job.getDataQualityReportId());
                printer.printRecord("lineageId", job.getDataLineageId());
                printer.printRecord("startedAt", job.getStartedAt());
                printer.printRecord("finishedAt", job.getFinishedAt());
                printer.printRecord("blockchain", "Voir /api/blockchain pour les blocs IMPORT_* associés");
            }
            return new FileSystemResource(report);
        } catch (IOException exception) {
            throw new BusinessException("Unable to generate import report: " + exception.getMessage());
        }
    }

    public String reportFileName(String jobId) {
        ImportJob job = jobEntity(jobId);
        return "import-report-" + safeFileName(job.getFileName()) + "-" + jobId + ".csv";
    }

    public int cleanupOldFiles(int days) {
        Instant threshold = Instant.now().minus(Duration.ofDays(Math.max(1, days)));
        int cleaned = 0;
        for (ImportJob job : importJobRepository.findAll()) {
            if (job.getFinishedAt() == null || job.getFinishedAt().isAfter(threshold) || job.getErrorFilePath() == null) {
                continue;
            }
            try {
                Files.deleteIfExists(Path.of(job.getErrorFilePath()));
                job.setErrorFilePath(null);
                importJobRepository.save(job);
                cleaned++;
            } catch (IOException ignored) {
            }
        }
        return cleaned;
    }

    private List<ImportDtos.ImportLineError> readErrorPreview(ImportJob job) {
        if (job.getErrorFilePath() == null || job.getErrorFilePath().isBlank()) {
            return List.of();
        }
        Path path = Path.of(job.getErrorFilePath());
        if (!Files.exists(path)) {
            return List.of();
        }
        List<ImportDtos.ImportLineError> errors = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            for (CSVRecord record : parser) {
                if (errors.size() >= MAX_ERRORS_IN_RESPONSE) {
                    break;
                }
                errors.add(new ImportDtos.ImportLineError(Long.parseLong(record.get("line")), record.get("message")));
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return errors;
    }

    private ImportDtos.ImportJobStartedResponse startImport(MultipartFile file, String type, String mode) {
        String jobId = UUID.randomUUID().toString();
        String userId = currentUserId();
        String originalName = sourceName(file);
        try {
            Path tempFile = Files.createTempFile("dataops-import-" + type.toLowerCase() + "-", ".csv");
            file.transferTo(tempFile);

            ImportJob job = new ImportJob();
            job.setId(jobId);
            job.setType(type);
            job.setMode(mode);
            job.setFileName(originalName);
            job.setImportedBy(userId == null || userId.isBlank() ? "system" : userId);
            job.setStatus("STARTING");
            job.setMessage("Import lancé");
            importJobRepository.save(job);

            CompletableFuture.runAsync(() -> runImportJob(jobId, type, mode, originalName, userId, tempFile));
            return new ImportDtos.ImportJobStartedResponse(jobId, "STARTING", 0, 0);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to start " + type.toLowerCase() + " import: " + exception.getMessage());
        }
    }

    private void runImportJob(String jobId, String type, String mode, String originalName, String userId, Path tempFile) {
        Path errorFile = null;
        try {
            ImportJob job = jobEntity(jobId);
            job.setStatus("COUNTING");
            job.setMessage("Comptage des lignes du fichier...");
            importJobRepository.save(job);

            long totalRows = countDataRows(tempFile);
            job.setTotalRows(totalRows);
            job.setStatus("RUNNING");
            job.setMessage("Traitement " + type + " : 0/" + totalRows + " lignes");
            importJobRepository.save(job);

            errorFile = Files.createTempFile("dataops-import-errors-" + jobId + "-", ".csv");
            ImportDtos.ImportResultResponse result;
            try (BufferedWriter writer = Files.newBufferedWriter(errorFile, StandardCharsets.UTF_8);
                 CSVPrinter errorPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader("line", "message").build());
                Reader reader = Files.newBufferedReader(tempFile, StandardCharsets.UTF_8)) {
                result = "SALES".equals(type)
                    ? importSales(originalName, reader, userId, jobId, errorPrinter, mode)
                    : importStock(originalName, reader, userId, jobId, errorPrinter, mode);
            }

            job = jobEntity(jobId);
            job.setStatus(cancelRequests.containsKey(jobId) ? "CANCELLED" : "COMPLETED");
            job.setMessage(cancelRequests.containsKey(jobId) ? "Import annulé" : originalName + " importé");
            job.setProcessedRows(Math.max(job.getProcessedRows(), job.getTotalRows()));
            job.setImportedRows(result.importedRows());
            job.setSkippedRows(result.skippedRows());
            job.setErrorCount(result.errors().size() < result.skippedRows() ? result.skippedRows() : result.errors().size());
            job.setDataQualityReportId(result.dataQualityReportId());
            job.setDataLineageId(result.dataLineageId());
            job.setErrorFilePath(result.skippedRows() > 0 && Files.size(errorFile) > 0 ? errorFile.toString() : null);
            job.setFinishedAt(Instant.now());
            importJobRepository.save(job);
        } catch (ImportCancelledException exception) {
            markFailedOrCancelled(jobId, "CANCELLED", "Import annulé", errorFile);
        } catch (Exception exception) {
            markFailedOrCancelled(jobId, "FAILED", exception.getMessage(), errorFile);
        } finally {
            cancelRequests.remove(jobId);
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
            }
        }
    }

    private ImportDtos.ImportResultResponse importSales(String sourceName, Reader reader, String userId, String jobId, CSVPrinter errorPrinter, String mode) {
        int imported = 0;
        int skipped = 0;
        List<ImportDtos.ImportLineError> errors = new ArrayList<>();
        QualityTracker quality = new QualityTracker();
        List<Sale> batch = new ArrayList<>(BATCH_SIZE);
        String importFileId = importFileId("sales", sourceName);

        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            validateHeaders(parser, SALES_COLUMNS);
            for (CSVRecord record : parser) {
                ensureNotCancelled(jobId);
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
                    if (saleRepository.existsBySaleDateAndAgency_CodeAndProduct_SkuAndQuantityAndUnitPrice(saleDate, value(record, "agencyCode"), value(record, "productCode"), quantity, unitPrice)) {
                        throw new IllegalArgumentException("Sale already exists in database");
                    }
                    Agency agency = agencyService.getByCode(value(record, "agencyCode"));
                    Product product = productService.getBySku(value(record, "productCode"));
                    validateSalesConsistency(quantity, unitPrice, quality);

                    Sale sale = new Sale();
                    sale.setAgency(agency);
                    sale.setProduct(product);
                    sale.setQuantity(quantity);
                    sale.setUnitPrice(unitPrice);
                    sale.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)));
                    sale.setSaleDate(saleDate);
                    sale.setReference("CSV_IMPORT_LINE_" + record.getRecordNumber());
                    batch.add(sale);
                    imported++;
                    if (batch.size() >= BATCH_SIZE) {
                        saleRepository.saveAll(batch);
                        blockchainService.addBlock("IMPORT_SALE_BATCH", "SALE_IMPORT", 0L, userId, "file=" + sourceName + "|batchSize=" + batch.size() + "|processed=" + (record.getRecordNumber() - 1));
                        batch.clear();
                    }
                } catch (RuntimeException exception) {
                    skipped++;
                    addError(errors, errorPrinter, record.getRecordNumber(), exception.getMessage());
                    failIfStrict(mode, exception.getMessage());
                } finally {
                    updateJobProgress(jobId, record.getRecordNumber() - 1, imported, skipped, errors.size());
                }
            }
            if (!batch.isEmpty()) {
                saleRepository.saveAll(batch);
                blockchainService.addBlock("IMPORT_SALE_BATCH", "SALE_IMPORT", 0L, userId, "file=" + sourceName + "|batchSize=" + batch.size() + "|final=true");
            }
        } catch (StrictImportException exception) {
            throw exception;
        } catch (Exception exception) {
            skipped++;
            addError(errors, errorPrinter, 0, "Unable to import sales CSV: " + exception.getMessage());
        }

        DataGovernanceService.ImportGovernanceResult governance = dataGovernanceService.recordImport(
            importFileId,
            sourceName,
            "CSV_SALES",
            "CSV_TO_SALE_ENTITIES_BATCH",
            quality.toMetrics(imported, skipped),
            userId
        );
        return new ImportDtos.ImportResultResponse(imported, skipped, errors, governance.reportId(), governance.lineageId());
    }

    private ImportDtos.ImportResultResponse importStock(String sourceName, Reader reader, String userId, String jobId, CSVPrinter errorPrinter, String mode) {
        int imported = 0;
        int skipped = 0;
        List<ImportDtos.ImportLineError> errors = new ArrayList<>();
        QualityTracker quality = new QualityTracker();
        List<StockMovement> batch = new ArrayList<>(BATCH_SIZE);
        String importFileId = importFileId("stocks", sourceName);

        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            validateHeaders(parser, STOCK_COLUMNS);
            for (CSVRecord record : parser) {
                ensureNotCancelled(jobId);
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
                    LocalDateTime movementDate = LocalDateTime.of(movementDay, LocalTime.MIDNIGHT);
                    if (stockMovementRepository.existsByMovementDateAndAgency_CodeAndProduct_SkuAndQuantityAndType(movementDate, value(record, "agencyCode"), value(record, "productCode"), quantity, type)) {
                        throw new IllegalArgumentException("Stock movement already exists in database");
                    }
                    Agency agency = agencyService.getByCode(value(record, "agencyCode"));
                    Product product = productService.getBySku(value(record, "productCode"));
                    validateStockConsistency(quantity, quality);

                    StockMovement movement = new StockMovement();
                    movement.setAgency(agency);
                    movement.setProduct(product);
                    movement.setType(type);
                    movement.setQuantity(quantity);
                    movement.setMovementDate(movementDate);
                    movement.setReason("CSV_IMPORT_LINE_" + record.getRecordNumber());
                    batch.add(movement);
                    imported++;
                    if (batch.size() >= BATCH_SIZE) {
                        stockMovementRepository.saveAll(batch);
                        blockchainService.addBlock("IMPORT_STOCK_BATCH", "STOCK_IMPORT", 0L, userId, "file=" + sourceName + "|batchSize=" + batch.size() + "|processed=" + (record.getRecordNumber() - 1));
                        batch.clear();
                    }
                } catch (RuntimeException exception) {
                    skipped++;
                    addError(errors, errorPrinter, record.getRecordNumber(), exception.getMessage());
                    failIfStrict(mode, exception.getMessage());
                } finally {
                    updateJobProgress(jobId, record.getRecordNumber() - 1, imported, skipped, errors.size());
                }
            }
            if (!batch.isEmpty()) {
                stockMovementRepository.saveAll(batch);
                blockchainService.addBlock("IMPORT_STOCK_BATCH", "STOCK_IMPORT", 0L, userId, "file=" + sourceName + "|batchSize=" + batch.size() + "|final=true");
            }
        } catch (StrictImportException exception) {
            throw exception;
        } catch (Exception exception) {
            skipped++;
            addError(errors, errorPrinter, 0, "Unable to import stock CSV: " + exception.getMessage());
        }

        DataGovernanceService.ImportGovernanceResult governance = dataGovernanceService.recordImport(
            importFileId,
            sourceName,
            "CSV_STOCK",
            "CSV_TO_STOCK_MOVEMENTS_BATCH",
            quality.toMetrics(imported, skipped),
            userId
        );
        return new ImportDtos.ImportResultResponse(imported, skipped, errors, governance.reportId(), governance.lineageId());
    }

    private void updateJobProgress(String jobId, long processedRows, int importedRows, int skippedRows, int visibleErrors) {
        if (jobId == null || processedRows % JOB_UPDATE_INTERVAL != 0) {
            return;
        }
        ImportJob job = importJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        job.setProcessedRows(Math.max(job.getProcessedRows(), processedRows));
        job.setImportedRows(importedRows);
        job.setSkippedRows(skippedRows);
        job.setErrorCount(Math.max(job.getErrorCount(), visibleErrors));
        job.setMessage("Traitement " + job.getType() + " : " + job.getProcessedRows() + "/" + job.getTotalRows() + " lignes");
        importJobRepository.save(job);
    }

    private void addError(List<ImportDtos.ImportLineError> errors, CSVPrinter errorPrinter, long line, String message) {
        if (errors.size() < MAX_ERRORS_IN_RESPONSE) {
            errors.add(new ImportDtos.ImportLineError(line, message));
        }
        if (errorPrinter != null) {
            try {
                errorPrinter.printRecord(line, message);
            } catch (IOException exception) {
                throw new BusinessException("Unable to write import error file: " + exception.getMessage());
            }
        }
    }

    private void failIfStrict(String mode, String message) {
        if ("STRICT_IMPORT".equals(mode)) {
            throw new StrictImportException("Strict import stopped: " + message);
        }
    }

    private String normalizeMode(String mode) {
        if ("STRICT_IMPORT".equalsIgnoreCase(String.valueOf(mode))) {
            return "STRICT_IMPORT";
        }
        return "PARTIAL_IMPORT";
    }

    private void markFailedOrCancelled(String jobId, String status, String message, Path errorFile) {
        ImportJob job = importJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        job.setStatus(status);
        job.setMessage(message == null || message.isBlank() ? "Import échoué" : message);
        job.setFinishedAt(Instant.now());
        if (errorFile != null) {
            try {
                job.setErrorFilePath(Files.exists(errorFile) && Files.size(errorFile) > 0 ? errorFile.toString() : null);
            } catch (IOException ignored) {
            }
        }
        importJobRepository.save(job);
    }

    private void ensureNotCancelled(String jobId) {
        if (jobId != null && cancelRequests.containsKey(jobId)) {
            throw new ImportCancelledException();
        }
    }

    private long countDataRows(Path path) throws IOException {
        try (var lines = Files.lines(path, StandardCharsets.UTF_8)) {
            return Math.max(0, lines.count() - 1);
        }
    }

    private ImportJob jobEntity(String jobId) {
        return importJobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Import job not found: " + jobId));
    }

    private ImportDtos.ImportJobSummaryResponse toSummaryResponse(ImportJob job) {
        return new ImportDtos.ImportJobSummaryResponse(
            job.getId(),
            job.getType(),
            job.getFileName(),
            job.getMode(),
            job.getStatus(),
            progress(job),
            job.getTotalRows(),
            job.getProcessedRows(),
            job.getImportedRows(),
            job.getSkippedRows(),
            job.getErrorCount(),
            job.getMessage(),
            rowsPerSecond(job),
            estimatedRemainingSeconds(job),
            errorDownloadUrl(job),
            job.getStartedAt(),
            job.getFinishedAt()
        );
    }

    private ImportDtos.ImportJobProgressResponse toProgressResponse(ImportJob job, ImportDtos.ImportResultResponse result) {
        return new ImportDtos.ImportJobProgressResponse(
            job.getId(),
            job.getType(),
            job.getFileName(),
            job.getMode(),
            job.getStatus(),
            progress(job),
            job.getTotalRows(),
            job.getProcessedRows(),
            job.getImportedRows(),
            job.getSkippedRows(),
            job.getErrorCount(),
            job.getMessage(),
            rowsPerSecond(job),
            estimatedRemainingSeconds(job),
            errorDownloadUrl(job),
            result,
            job.getStartedAt(),
            job.getFinishedAt()
        );
    }

    private int progress(ImportJob job) {
        if ("COMPLETED".equals(job.getStatus())) {
            return 100;
        }
        if (job.getTotalRows() == null || job.getTotalRows() == 0) {
            return 0;
        }
        return (int) Math.min(99, Math.round((job.getProcessedRows() * 100.0) / job.getTotalRows()));
    }

    private double rowsPerSecond(ImportJob job) {
        long seconds = Math.max(1, Duration.between(job.getStartedAt(), job.getFinishedAt() == null ? Instant.now() : job.getFinishedAt()).toSeconds());
        return Math.round((job.getProcessedRows() / (double) seconds) * 10.0) / 10.0;
    }

    private Long estimatedRemainingSeconds(ImportJob job) {
        if (job.getTotalRows() == null || job.getTotalRows() == 0 || job.getProcessedRows() >= job.getTotalRows()) {
            return 0L;
        }
        double speed = rowsPerSecond(job);
        if (speed <= 0) {
            return null;
        }
        return Math.round((job.getTotalRows() - job.getProcessedRows()) / speed);
    }

    private String errorDownloadUrl(ImportJob job) {
        return job.getErrorFilePath() == null || job.getErrorFilePath().isBlank() ? null : "/api/import/jobs/" + job.getId() + "/errors";
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

    private void validatePreviewSales(CSVRecord record) {
        requirePreview(record, "date", "agencyCode", "productCode", "quantity", "unitPrice");
        LocalDate.parse(value(record, "date"));
        int quantity = Integer.parseInt(value(record, "quantity"));
        BigDecimal unitPrice = new BigDecimal(value(record, "unitPrice"));
        if (quantity <= 0 || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Inconsistent sales values");
        }
    }

    private void validatePreviewStock(CSVRecord record) {
        requirePreview(record, "date", "agencyCode", "productCode", "quantity", "type");
        LocalDate.parse(value(record, "date"));
        int quantity = Integer.parseInt(value(record, "quantity"));
        StockMovementType.valueOf(value(record, "type").toUpperCase());
        if (quantity <= 0) {
            throw new IllegalArgumentException("Inconsistent stock quantity");
        }
    }

    private void requirePreview(CSVRecord record, String... columns) {
        for (String column : columns) {
            if (value(record, column).isBlank()) {
                throw new IllegalArgumentException("Missing required value: " + column);
            }
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
        return prefix + "-" + safeFileName(sourceName) + "-" + Instant.now().toEpochMilli();
    }

    private String sourceName(MultipartFile file) {
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            return "import.csv";
        }
        return file.getOriginalFilename();
    }

    private String safeFileName(String sourceName) {
        return sourceName.replaceAll("[^a-zA-Z0-9._-]", "_");
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

    private static final class ImportCancelledException extends RuntimeException {
    }

    private static final class StrictImportException extends RuntimeException {
        private StrictImportException(String message) {
            super(message);
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
