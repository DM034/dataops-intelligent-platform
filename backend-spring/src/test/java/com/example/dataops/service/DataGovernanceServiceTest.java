package com.example.dataops.service;

import com.example.dataops.dto.DataGovernanceDtos;
import com.example.dataops.mapper.DataopsMapper;
import com.example.dataops.model.BlockchainBlock;
import com.example.dataops.model.DataCatalog;
import com.example.dataops.model.DataLineage;
import com.example.dataops.model.DataQualityReport;
import com.example.dataops.model.ImportAudit;
import com.example.dataops.repository.BlockchainRepository;
import com.example.dataops.repository.DataCatalogRepository;
import com.example.dataops.repository.DataLineageRepository;
import com.example.dataops.repository.DataQualityReportRepository;
import com.example.dataops.repository.ImportAuditRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataGovernanceServiceTest {
    private final DataQualityReportRepository qualityRepository = mock(DataQualityReportRepository.class);
    private final DataLineageRepository lineageRepository = mock(DataLineageRepository.class);
    private final DataCatalogRepository catalogRepository = mock(DataCatalogRepository.class);
    private final ImportAuditRepository auditRepository = mock(ImportAuditRepository.class);
    private final BlockchainRepository blockchainRepository = mock(BlockchainRepository.class);
    private final DataopsMapper mapper = new DataopsMapper();
    private final BlockchainService blockchainService = new BlockchainService(blockchainRepository, mapper);
    private final DataGovernanceService service = new DataGovernanceService(
        qualityRepository,
        lineageRepository,
        catalogRepository,
        auditRepository,
        mapper,
        blockchainService
    );

    @Test
    void createCatalogStoresGovernanceMetadata() {
        when(catalogRepository.existsByName("VENTES")).thenReturn(false);
        when(catalogRepository.save(any(DataCatalog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DataGovernanceDtos.DataCatalogResponse response = service.createCatalog(new DataGovernanceDtos.DataCatalogRequest(
            "VENTES",
            "CSV",
            "Ventes importees",
            "Data owner",
            "Quotidienne"
        ));

        assertThat(response.name()).isEqualTo("VENTES");
        assertThat(response.sourceType()).isEqualTo("CSV");
        assertThat(response.owner()).isEqualTo("Data owner");
    }

    @Test
    void recordImportCreatesQualityLineageAuditAndBlockchainBlock() {
        when(qualityRepository.save(any(DataQualityReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lineageRepository.save(any(DataLineage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any(ImportAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(blockchainRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(blockchainRepository.save(any(BlockchainBlock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordImport(
            "sales-demo-1",
            "sales.csv",
            "CSV_SALES",
            "CSV_TO_SALE_ENTITIES",
            new DataGovernanceService.QualityMetrics(10, 8, 2, 1, 9, 8, 9, 8),
            "admin"
        );

        ArgumentCaptor<DataQualityReport> reportCaptor = ArgumentCaptor.forClass(DataQualityReport.class);
        ArgumentCaptor<ImportAudit> auditCaptor = ArgumentCaptor.forClass(ImportAudit.class);
        verify(qualityRepository).save(reportCaptor.capture());
        verify(auditRepository).save(auditCaptor.capture());
        verify(blockchainRepository).save(any(BlockchainBlock.class));

        DataQualityReport report = reportCaptor.getValue();
        assertThat(report.getSourceName()).isEqualTo("sales.csv");
        assertThat(report.getTotalRecords()).isEqualTo(10);
        assertThat(report.getDuplicateRecords()).isEqualTo(1);
        assertThat(report.getQualityScore()).isNotNull();

        ImportAudit audit = auditCaptor.getValue();
        assertThat(audit.getFileName()).isEqualTo("sales.csv");
        assertThat(audit.getImportedBy()).isEqualTo("admin");
        assertThat(audit.getSuccessRows()).isEqualTo(8);
        assertThat(audit.getFailedRows()).isEqualTo(2);
    }
}
