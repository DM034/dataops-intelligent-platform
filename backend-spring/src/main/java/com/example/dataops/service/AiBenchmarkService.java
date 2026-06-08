package com.example.dataops.service;

import com.example.dataops.dto.AiDtos;
import com.example.dataops.model.Sale;
import com.example.dataops.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

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
        List<AiDtos.BenchmarkSalePoint> sales = saleRepository.findAll().stream()
            .sorted(Comparator.comparing(Sale::getSaleDate))
            .map(sale -> new AiDtos.BenchmarkSalePoint(
                sale.getSaleDate(),
                sale.getAgency().getCode(),
                sale.getProduct().getSku(),
                sale.getQuantity(),
                sale.getTotalAmount()
            ))
            .toList();

        return aiClientService.benchmarkAnomalies(sales);
    }
}
