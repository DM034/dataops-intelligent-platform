package com.example.dataops.service;

import com.example.dataops.dto.DataGovernanceDtos;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.DataLineage;
import com.example.dataops.model.DataQualityReport;
import com.example.dataops.repository.DataLineageRepository;
import com.example.dataops.repository.DataQualityReportRepository;
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
    private final DataopsMapper mapper;

    public DataGovernanceService(DataQualityReportRepository qualityReportRepository, DataLineageRepository lineageRepository, DataopsMapper mapper) {
        this.qualityReportRepository = qualityReportRepository;
        this.lineageRepository = lineageRepository;
        this.mapper = mapper;
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

    @Transactional
    public ImportGovernanceResult recordImport(String importFileId, String sourceName, String sourceType, String transformationStep, QualityMetrics metrics) {
        DataQualityReport report = new DataQualityReport();
        report.setImportFileId(importFileId);
        report.setTotalRows(metrics.totalRows());
        report.setValidRows(metrics.validRows());
        report.setErrorRows(metrics.errorRows());
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
        lineage.setValidationStep(metrics.errorRows() == 0 ? "VALIDATED" : "VALIDATED_WITH_ERRORS");
        lineage.setTransformationStep(transformationStep);
        lineage.setStorageStep(metrics.validRows() > 0 ? "POSTGRESQL_PERSISTED" : "POSTGRESQL_NOT_UPDATED");
        lineage.setDashboardStep(metrics.validRows() > 0 ? "DASHBOARD_READY" : "DASHBOARD_BLOCKED");
        lineage.setStatus(status(metrics));
        DataLineage savedLineage = lineageRepository.save(lineage);

        return new ImportGovernanceResult(savedReport.getId(), savedLineage.getId());
    }

    private BigDecimal rate(int value, int total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(value)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal globalScore(DataQualityReport report) {
        return report.getCompletenessRate()
            .add(report.getValidityRate())
            .add(report.getUniquenessRate())
            .add(report.getConsistencyRate())
            .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
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
        int completeRows,
        int validFormatRows,
        int uniqueRows,
        int consistentRows
    ) {
    }

    public record ImportGovernanceResult(Long reportId, Long lineageId) {
    }
}
