package com.example.dataops.service;

import com.example.dataops.dto.AiDtos;
import com.example.dataops.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class AiBenchmarkService {
    private final SaleRepository saleRepository;
    private final AiClientService aiClientService;

    public AiBenchmarkService(SaleRepository saleRepository, AiClientService aiClientService) {
        this.saleRepository = saleRepository;
        this.aiClientService = aiClientService;
    }

    @Transactional(readOnly = true)
    public AiDtos.BenchmarkAnomalyResponse benchmarkSalesAnomalies() {
        List<AiDtos.BenchmarkSalePoint> sales = saleRepository.benchmarkSalesSeries().stream()
            .map(this::toBenchmarkPoint)
            .filter(Objects::nonNull)
            .toList();

        return aiClientService.benchmarkAnomalies(sales);
    }

    private AiDtos.BenchmarkSalePoint toBenchmarkPoint(Object[] row) {
        LocalDate date = (LocalDate) row[0];
        String agencyCode = (String) row[1];
        String productCode = (String) row[2];
        Number quantity = (Number) row[3];
        BigDecimal amount = (BigDecimal) row[4];

        if (date == null || agencyCode == null || productCode == null || quantity == null || amount == null) {
            return null;
        }
        if (quantity.intValue() < 0 || amount.compareTo(BigDecimal.ZERO) < 0) {
            return null;
        }

        return new AiDtos.BenchmarkSalePoint(
            date.toString(),
            agencyCode,
            productCode,
            quantity.intValue(),
            amount
        );
    }
}
