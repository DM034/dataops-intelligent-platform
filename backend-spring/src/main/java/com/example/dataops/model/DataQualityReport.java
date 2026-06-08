package com.example.dataops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "data_quality_reports")
public class DataQualityReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String importFileId;

    @Column(nullable = false)
    private String sourceName;

    @Column(nullable = false)
    private Integer totalRows;

    @Column(nullable = false)
    private Integer validRows;

    @Column(nullable = false)
    private Integer errorRows;

    @Column(nullable = false)
    private Integer duplicateRecords = 0;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal completenessRate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal validityRate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal uniquenessRate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal consistencyRate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal globalScore;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getImportFileId() {
        return importFileId;
    }

    public void setImportFileId(String importFileId) {
        this.importFileId = importFileId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getValidRows() {
        return validRows;
    }

    public void setValidRows(Integer validRows) {
        this.validRows = validRows;
    }

    public Integer getErrorRows() {
        return errorRows;
    }

    public void setErrorRows(Integer errorRows) {
        this.errorRows = errorRows;
    }

    public Integer getDuplicateRecords() {
        return duplicateRecords;
    }

    public void setDuplicateRecords(Integer duplicateRecords) {
        this.duplicateRecords = duplicateRecords;
    }

    public BigDecimal getCompletenessRate() {
        return completenessRate;
    }

    public void setCompletenessRate(BigDecimal completenessRate) {
        this.completenessRate = completenessRate;
    }

    public BigDecimal getValidityRate() {
        return validityRate;
    }

    public void setValidityRate(BigDecimal validityRate) {
        this.validityRate = validityRate;
    }

    public BigDecimal getUniquenessRate() {
        return uniquenessRate;
    }

    public void setUniquenessRate(BigDecimal uniquenessRate) {
        this.uniquenessRate = uniquenessRate;
    }

    public BigDecimal getConsistencyRate() {
        return consistencyRate;
    }

    public void setConsistencyRate(BigDecimal consistencyRate) {
        this.consistencyRate = consistencyRate;
    }

    public BigDecimal getGlobalScore() {
        return globalScore;
    }

    public void setGlobalScore(BigDecimal globalScore) {
        this.globalScore = globalScore;
    }

    public Integer getTotalRecords() {
        return totalRows;
    }

    public Integer getValidRecords() {
        return validRows;
    }

    public Integer getInvalidRecords() {
        return errorRows;
    }

    public BigDecimal getQualityScore() {
        return globalScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
