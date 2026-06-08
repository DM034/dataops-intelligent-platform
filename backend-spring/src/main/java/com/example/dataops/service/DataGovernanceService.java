package com.example.dataops.service;

import com.example.dataops.dto.DataGovernanceDtos;
import com.example.dataops.exception.BusinessException;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.DataCatalog;
import com.example.dataops.model.DataLineage;
import com.example.dataops.model.DataQualityReport;
import com.example.dataops.model.ImportAudit;
import com.example.dataops.repository.DataCatalogRepository;
import com.example.dataops.repository.DataLineageRepository;
import com.example.dataops.repository.DataQualityReportRepository;
import com.example.dataops.repository.ImportAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class DataGovernanceService {
    private final DataQualityReportRepository qualityReportRepository;
    private final DataLineageRepository lineageRepository;
    private final DataCatalogRepository catalogRepository;
    private final ImportAuditRepository importAuditRepository;
    private final DataopsMapper mapper;
    private final BlockchainService blockchainService;

    public DataGovernanceService(
        DataQualityReportRepository qualityReportRepository,
        DataLineageRepository lineageRepository,
        DataCatalogRepository catalogRepository,
        ImportAuditRepository importAuditRepository,
        DataopsMapper mapper,
        BlockchainService blockchainService
    ) {
        this.qualityReportRepository = qualityReportRepository;
        this.lineageRepository = lineageRepository;
        this.catalogRepository = catalogRepository;
        this.importAuditRepository = importAuditRepository;
        this.mapper = mapper;
        this.blockchainService = blockchainService;
    }

    @Transactional
    public List<DataGovernanceDtos.DataCatalogResponse> catalog() {
        ensureDefaultCatalog();
        return catalogRepository.findAll().stream()
            .sorted(Comparator.comparing(DataCatalog::getName))
            .map(mapper::toDataCatalogResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public DataGovernanceDtos.DataCatalogResponse catalog(Long id) {
        return mapper.toDataCatalogResponse(catalogEntity(id));
    }

    @Transactional
    public DataGovernanceDtos.DataCatalogResponse createCatalog(DataGovernanceDtos.DataCatalogRequest request) {
        if (catalogRepository.existsByName(request.name())) {
            throw new BusinessException("Catalog entry already exists: " + request.name());
        }
        DataCatalog catalog = new DataCatalog();
        applyCatalog(request, catalog);
        return mapper.toDataCatalogResponse(catalogRepository.save(catalog));
    }

    @Transactional
    public DataGovernanceDtos.DataCatalogResponse updateCatalog(Long id, DataGovernanceDtos.DataCatalogRequest request) {
        DataCatalog catalog = catalogEntity(id);
        applyCatalog(request, catalog);
        return mapper.toDataCatalogResponse(catalog);
    }

    @Transactional
    public void deleteCatalog(Long id) {
        catalogRepository.delete(catalogEntity(id));
    }

    @Transactional(readOnly = true)
    public List<DataGovernanceDtos.DataQualityReportResponse> reports() {
        return qualityReportRepository.findAll().stream()
            .sorted(Comparator.comparing(DataQualityReport::getCreatedAt).reversed())
            .map(mapper::toDataQualityReportResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public DataGovernanceDtos.DataQualityReportResponse report(Long id) {
        return mapper.toDataQualityReportResponse(qualityReportRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Data quality report not found")));
    }

    @Transactional(readOnly = true)
    public DataGovernanceDtos.DataQualityReportResponse latestReport() {
        return qualityReportRepository.findAll().stream()
            .max(Comparator.comparing(DataQualityReport::getCreatedAt))
            .map(mapper::toDataQualityReportResponse)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<DataGovernanceDtos.DataLineageResponse> lineage() {
        return lineageRepository.findAll().stream()
            .sorted(Comparator.comparing(DataLineage::getImportDate).reversed())
            .map(mapper::toDataLineageResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public DataGovernanceDtos.DataLineageResponse lineage(Long id) {
        return mapper.toDataLineageResponse(lineageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Data lineage not found")));
    }

    @Transactional(readOnly = true)
    public List<DataGovernanceDtos.ImportAuditResponse> importAudits() {
        return importAuditRepository.findAllByOrderByImportDateDesc().stream()
            .map(mapper::toImportAuditResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public DataGovernanceDtos.ImportAuditResponse importAudit(Long id) {
        return mapper.toImportAuditResponse(importAuditRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Import audit not found")));
    }

    @Transactional
    public DataGovernanceDtos.GovernanceDashboardResponse dashboard() {
        List<DataGovernanceDtos.DataQualityReportResponse> history = reports();
        DataGovernanceDtos.DataQualityReportResponse latest = history.isEmpty() ? null : history.get(0);
        List<DataGovernanceDtos.ImportAuditResponse> imports = importAudits();
        return new DataGovernanceDtos.GovernanceDashboardResponse(
            latest == null ? BigDecimal.ZERO.setScale(2) : latest.qualityScore(),
            latest == null ? 0 : latest.invalidRecords(),
            latest == null ? 0 : latest.duplicateRecords(),
            imports.size(),
            latest == null ? BigDecimal.ZERO.setScale(2) : latest.completenessRate(),
            latest == null ? BigDecimal.ZERO.setScale(2) : latest.validityRate(),
            history,
            lineage(),
            catalog(),
            imports
        );
    }

    @Transactional
    public ImportGovernanceResult recordImport(String importFileId, String sourceName, String sourceType, String transformationStep, QualityMetrics metrics, String importedBy) {
        DataQualityReport report = new DataQualityReport();
        report.setImportFileId(importFileId);
        report.setSourceName(sourceName);
        report.setTotalRows(metrics.totalRows());
        report.setValidRows(metrics.validRows());
        report.setErrorRows(metrics.errorRows());
        report.setDuplicateRecords(metrics.duplicateRows());
        report.setCompletenessRate(rate(metrics.completeRows(), metrics.totalRows()));
        report.setValidityRate(rate(metrics.validFormatRows(), metrics.totalRows()));
        report.setUniquenessRate(rate(metrics.uniqueRows(), metrics.totalRows()));
        report.setConsistencyRate(rate(metrics.consistentRows(), metrics.totalRows()));
        report.setGlobalScore(globalScore(report));
        DataQualityReport savedReport = qualityReportRepository.save(report);

        DataLineage lineage = new DataLineage();
        lineage.setSourceName(sourceName);
        lineage.setSourceType(sourceType);
        lineage.setImportDate(Instant.now());
        lineage.setValidationDate(Instant.now());
        lineage.setTransformationDate(Instant.now());
        lineage.setStorageDate(metrics.validRows() > 0 ? Instant.now() : null);
        lineage.setDashboardDate(metrics.validRows() > 0 ? Instant.now() : null);
        lineage.setValidationStep(metrics.errorRows() == 0 ? "VALIDATED" : "VALIDATED_WITH_ERRORS");
        lineage.setTransformationStep(transformationStep);
        lineage.setStorageStep(metrics.validRows() > 0 ? "POSTGRESQL_PERSISTED" : "POSTGRESQL_NOT_UPDATED");
        lineage.setDashboardStep(metrics.validRows() > 0 ? "DASHBOARD_READY" : "DASHBOARD_BLOCKED");
        lineage.setStatus(status(metrics));
        DataLineage savedLineage = lineageRepository.save(lineage);

        ImportAudit audit = new ImportAudit();
        audit.setFileName(sourceName);
        audit.setImportedBy(importedBy == null || importedBy.isBlank() ? "system" : importedBy);
        audit.setTotalRows(metrics.totalRows());
        audit.setSuccessRows(metrics.validRows());
        audit.setFailedRows(metrics.errorRows());
        audit.setStatus(status(metrics));
        ImportAudit savedAudit = importAuditRepository.save(audit);

        String payload = "fileName=" + sourceName
            + "|importDate=" + savedAudit.getImportDate()
            + "|totalRows=" + metrics.totalRows()
            + "|qualityScore=" + savedReport.getQualityScore();
        blockchainService.addBlock("IMPORT_GOVERNANCE", "IMPORT", savedAudit.getId(), savedAudit.getImportedBy(), payload);

        return new ImportGovernanceResult(savedReport.getId(), savedLineage.getId());
    }

    private BigDecimal rate(int value, int total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(Math.min(value, total))
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal globalScore(DataQualityReport report) {
        return report.getCompletenessRate()
            .multiply(BigDecimal.valueOf(0.30))
            .add(report.getValidityRate().multiply(BigDecimal.valueOf(0.25)))
            .add(report.getUniquenessRate().multiply(BigDecimal.valueOf(0.20)))
            .add(report.getConsistencyRate().multiply(BigDecimal.valueOf(0.25)))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private String status(QualityMetrics metrics) {
        if (metrics.validRows() == 0 && (metrics.totalRows() > 0 || metrics.errorRows() > 0)) {
            return "FAILED";
        }
        if (metrics.errorRows() > 0) {
            return "PARTIAL_SUCCESS";
        }
        return "SUCCESS";
    }

    public record QualityMetrics(
        int totalRows,
        int validRows,
        int errorRows,
        int duplicateRows,
        int completeRows,
        int validFormatRows,
        int uniqueRows,
        int consistentRows
    ) {
    }

    public record ImportGovernanceResult(Long reportId, Long lineageId) {
    }

    private DataCatalog catalogEntity(Long id) {
        return catalogRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Catalog entry not found: " + id));
    }

    private void applyCatalog(DataGovernanceDtos.DataCatalogRequest request, DataCatalog catalog) {
        catalog.setName(request.name());
        catalog.setSourceType(request.sourceType());
        catalog.setDescription(request.description());
        catalog.setOwner(request.owner());
        catalog.setRefreshFrequency(request.refreshFrequency());
    }

    private void ensureDefaultCatalog() {
        if (catalogRepository.count() > 0) {
            return;
        }
        createDefaultCatalog("VENTES", "CSV/API", "Ventes consolidees importees et saisies dans la plateforme.", "Direction commerciale", "Quotidienne");
        createDefaultCatalog("STOCKS", "CSV/API", "Mouvements et niveaux de stock par agence et produit.", "Operations", "Quotidienne");
        createDefaultCatalog("PRODUITS", "API", "Referentiel produits, categories et prix unitaires.", "Direction produit", "Hebdomadaire");
        createDefaultCatalog("AGENCES", "API", "Referentiel des agences commerciales et points de vente.", "Administration", "Mensuelle");
    }

    private void createDefaultCatalog(String name, String sourceType, String description, String owner, String refreshFrequency) {
        DataCatalog catalog = new DataCatalog();
        catalog.setName(name);
        catalog.setSourceType(sourceType);
        catalog.setDescription(description);
        catalog.setOwner(owner);
        catalog.setRefreshFrequency(refreshFrequency);
        catalogRepository.save(catalog);
    }
}
