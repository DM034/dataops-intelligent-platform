package com.example.dataops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "data_lineage")
public class DataLineage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sourceName;

    @Column(nullable = false)
    private String sourceType;

    @Column(nullable = false)
    private Instant importDate;

    private Instant validationDate;

    private Instant transformationDate;

    private Instant storageDate;

    private Instant dashboardDate;

    @Column(nullable = false)
    private String validationStep;

    @Column(nullable = false)
    private String transformationStep;

    @Column(nullable = false)
    private String storageStep;

    @Column(nullable = false)
    private String dashboardStep;

    @Column(nullable = false)
    private String status;

    public Long getId() {
        return id;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSource() {
        return sourceName;
    }

    public void setSource(String source) {
        this.sourceName = source;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Instant getImportDate() {
        return importDate;
    }

    public void setImportDate(Instant importDate) {
        this.importDate = importDate;
    }

    public Instant getValidationDate() {
        return validationDate;
    }

    public void setValidationDate(Instant validationDate) {
        this.validationDate = validationDate;
    }

    public Instant getTransformationDate() {
        return transformationDate;
    }

    public void setTransformationDate(Instant transformationDate) {
        this.transformationDate = transformationDate;
    }

    public Instant getStorageDate() {
        return storageDate;
    }

    public void setStorageDate(Instant storageDate) {
        this.storageDate = storageDate;
    }

    public Instant getDashboardDate() {
        return dashboardDate;
    }

    public void setDashboardDate(Instant dashboardDate) {
        this.dashboardDate = dashboardDate;
    }

    public String getValidationStep() {
        return validationStep;
    }

    public void setValidationStep(String validationStep) {
        this.validationStep = validationStep;
    }

    public String getTransformationStep() {
        return transformationStep;
    }

    public void setTransformationStep(String transformationStep) {
        this.transformationStep = transformationStep;
    }

    public String getStorageStep() {
        return storageStep;
    }

    public void setStorageStep(String storageStep) {
        this.storageStep = storageStep;
    }

    public String getDashboardStep() {
        return dashboardStep;
    }

    public void setDashboardStep(String dashboardStep) {
        this.dashboardStep = dashboardStep;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
