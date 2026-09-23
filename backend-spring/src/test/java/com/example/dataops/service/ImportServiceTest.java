package com.example.dataops.service;

import com.example.dataops.dto.ImportDtos;
import com.example.dataops.repository.DatasetVersionRepository;
import com.example.dataops.repository.ImportJobRepository;
import com.example.dataops.repository.SaleRepository;
import com.example.dataops.repository.StockMovementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ImportServiceTest {
    private final ImportService service = new ImportService(
        mock(SaleRepository.class),
        mock(StockMovementRepository.class),
        mock(ImportJobRepository.class),
        mock(DatasetVersionRepository.class),
        mock(AgencyService.class),
        mock(ProductService.class),
        mock(BlockchainService.class),
        mock(DataGovernanceService.class)
    );

    @Test
    void previewDetectsSalesCsv() {
        MockMultipartFile file = csv("sales.csv", """
            date,agencyCode,productCode,quantity,unitPrice
            2026-01-01,TANA,P001,2,1500
            """);

        ImportDtos.ImportPreviewResponse preview = service.preview(file);

        assertThat(preview.detectedType()).isEqualTo("SALES");
        assertThat(preview.status()).isEqualTo("VALID");
        assertThat(preview.totalRows()).isEqualTo(1);
        assertThat(preview.missingColumns()).isEmpty();
    }

    @Test
    void previewReportsMissingColumns() {
        MockMultipartFile file = csv("unknown.csv", """
            date,agencyCode,quantity
            2026-01-01,TANA,2
            """);

        ImportDtos.ImportPreviewResponse preview = service.preview(file);

        assertThat(preview.status()).isEqualTo("INVALID");
        assertThat(preview.detectedType()).isEqualTo("UNKNOWN");
        assertThat(preview.missingColumns()).isNotEmpty();
    }

    private MockMultipartFile csv(String name, String content) {
        return new MockMultipartFile("file", name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
