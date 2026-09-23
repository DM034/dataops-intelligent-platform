package com.example.dataops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "import_jobs")
public class ImportJob {
    @Id
    private String id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String mode = "PARTIAL_IMPORT";

    @Column(nullable = false)
    private String importedBy;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private Long totalRows = 0L;

    @Column(nullable = false)
    private Long processedRows = 0L;

    @Column(nullable = false)
    private Integer importedRows = 0;

    @Column(nullable = false)
    private Integer skippedRows = 0;

    @Column(nullable = false)
    private Integer errorCount = 0;

    private Long dataQualityReportId;

    private Long dataLineageId;

    private String errorFilePath;

    @Column(nullable = false)
    private Instant startedAt = Instant.now();

    private Instant finishedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getImportedBy() {
        return importedBy;
    }

    public void setImportedBy(String importedBy) {
        this.importedBy = importedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Long totalRows) {
        this.totalRows = totalRows;
    }

    public Long getProcessedRows() {
        return processedRows;
    }

    public void setProcessedRows(Long processedRows) {
        this.processedRows = processedRows;
    }

    public Integer getImportedRows() {
        return importedRows;
    }

    public void setImportedRows(Integer importedRows) {
        this.importedRows = importedRows;
    }

    public Integer getSkippedRows() {
        return skippedRows;
    }

    public void setSkippedRows(Integer skippedRows) {
        this.skippedRows = skippedRows;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public Long getDataQualityReportId() {
        return dataQualityReportId;
    }

    public void setDataQualityReportId(Long dataQualityReportId) {
        this.dataQualityReportId = dataQualityReportId;
    }

    public Long getDataLineageId() {
        return dataLineageId;
    }

    public void setDataLineageId(Long dataLineageId) {
        this.dataLineageId = dataLineageId;
    }

    public String getErrorFilePath() {
        return errorFilePath;
    }

    public void setErrorFilePath(String errorFilePath) {
        this.errorFilePath = errorFilePath;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
