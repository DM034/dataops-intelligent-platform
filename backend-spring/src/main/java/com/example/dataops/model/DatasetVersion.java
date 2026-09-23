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
@Table(name = "dataset_versions")
public class DatasetVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String datasetName;

    @Column(nullable = false)
    private String importJobId;

    @Column(nullable = false)
    private String sourceFileName;

    @Column(nullable = false, length = 128)
    private String sourceFileHash;

    @Column(nullable = false)
    private String schemaVersion;

    @Column(nullable = false)
    private String status;

    @Column(precision = 8, scale = 2)
    private BigDecimal qualityScore;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getImportJobId() {
        return importJobId;
    }

    public void setImportJobId(String importJobId) {
        this.importJobId = importJobId;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getSourceFileHash() {
        return sourceFileHash;
    }

    public void setSourceFileHash(String sourceFileHash) {
        this.sourceFileHash = sourceFileHash;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(BigDecimal qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
