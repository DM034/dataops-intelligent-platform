package com.example.dataops.service;

import com.example.dataops.dto.AiDtos;
import com.example.dataops.model.DataQualityReport;
import com.example.dataops.model.Sale;
import com.example.dataops.model.StockMovement;
import com.example.dataops.repository.DataQualityReportRepository;
import com.example.dataops.repository.SaleRepository;
import com.example.dataops.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AiDecisionIntelligenceService {
    private final SaleRepository saleRepository;
    private final StockMovementRepository stockMovementRepository;
    private final DataQualityReportRepository dataQualityReportRepository;
    private final AiClientService aiClientService;

    public AiDecisionIntelligenceService(
        SaleRepository saleRepository,
        StockMovementRepository stockMovementRepository,
        DataQualityReportRepository dataQualityReportRepository,
        AiClientService aiClientService
    ) {
        this.saleRepository = saleRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.dataQualityReportRepository = dataQualityReportRepository;
        this.aiClientService = aiClientService;
    }

    @Transactional(readOnly = true)
    public AiDtos.DecisionIntelligenceResponse analyze() {
        AiDtos.DecisionIntelligenceRequest request = new AiDtos.DecisionIntelligenceRequest(
            sales(),
            stocks(),
            sourceScores(),
            new AiDtos.WhatIfScenario(30, 7)
        );

        try {
            return aiClientService.decisionIntelligence(request);
        } catch (RestClientException exception) {
            return fallbackResponse(request, exception.getMessage());
        }
    }

    public Map<String, Object> modelsStatus() {
        try {
            return aiClientService.modelsStatus();
        } catch (RestClientException exception) {
            return Map.of(
                "service", "ai-service",
                "status", "unavailable",
                "error", exception.getMessage()
            );
        }
    }

    private List<AiDtos.DecisionSalePoint> sales() {
        return saleRepository.findAll().stream()
            .sorted(Comparator.comparing(Sale::getSaleDate))
            .map(sale -> new AiDtos.DecisionSalePoint(
                sale.getSaleDate(),
                sale.getAgency().getCode(),
                sale.getProduct().getSku(),
                sale.getQuantity(),
                sale.getTotalAmount()
            ))
            .toList();
    }

    private List<AiDtos.DecisionStockPoint> stocks() {
        return stockMovementRepository.findAll().stream()
            .sorted(Comparator.comparing(StockMovement::getMovementDate))
            .map(movement -> new AiDtos.DecisionStockPoint(
                movement.getMovementDate().toLocalDate(),
                movement.getAgency().getCode(),
                movement.getProduct().getSku(),
                movement.getQuantity(),
                movement.getType().name()
            ))
            .toList();
    }

    private List<AiDtos.SourceQualitySnapshot> sourceScores() {
        return dataQualityReportRepository.findAll().stream()
            .sorted(Comparator.comparing(DataQualityReport::getCreatedAt).reversed())
            .limit(20)
            .map(report -> new AiDtos.SourceQualitySnapshot(
                report.getSourceName(),
                report.getGlobalScore(),
                report.getTotalRows(),
                report.getErrorRows()
            ))
            .toList();
    }

    private AiDtos.DecisionIntelligenceResponse fallbackResponse(AiDtos.DecisionIntelligenceRequest request, String error) {
        Map<String, Object> summary = Map.of(
            "analysisDate", LocalDate.now().toString(),
            "salesRows", request.sales().size(),
            "stockRows", request.stocks().size(),
            "moduleCount", 15,
            "globalRiskLevel", "UNKNOWN",
            "mode", "fallback"
        );
        return new AiDtos.DecisionIntelligenceResponse(
            summary,
            Map.of(
                "serviceUnavailable", Map.of(
                    "message", "Le service IA FastAPI est indisponible.",
                    "detail", error == null ? "Erreur inconnue" : error
                )
            ),
            List.of(),
            List.of(Map.of(
                "type", "SYSTEM",
                "priority", "WARNING",
                "message", "Redemarrer ai-service puis relancer l'analyse.",
                "explanation", "Le backend a prepare les donnees mais n'a pas pu joindre FastAPI."
            )),
            Map.of("service", "ai-service", "status", "unavailable")
        );
    }
}
