package com.example.dataops.service;

import com.example.dataops.dto.AiDtos;
import com.example.dataops.dto.AlertDtos;
import com.example.dataops.model.AlertSeverity;
import com.example.dataops.model.Sale;
import com.example.dataops.model.StockMovement;
import com.example.dataops.model.StockMovementType;
import com.example.dataops.repository.SaleRepository;
import com.example.dataops.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AiAnalysisService {
    private static final double DEFAULT_ZSCORE_THRESHOLD = 2.0;
    private static final long DEFAULT_REORDER_THRESHOLD = 10;

    private final AiClientService aiClientService;
    private final SaleRepository saleRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AlertService alertService;

    public AiAnalysisService(
        AiClientService aiClientService,
        SaleRepository saleRepository,
        StockMovementRepository stockMovementRepository,
        AlertService alertService
    ) {
        this.aiClientService = aiClientService;
        this.saleRepository = saleRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.alertService = alertService;
    }

    @Transactional
    public AiDtos.SalesAnomalyAnalysisResponse analyzeSalesAnomalies() {
        List<AiDtos.SalePoint> salePoints = saleRepository.findAll().stream()
            .sorted(Comparator.comparing(Sale::getSaleDate))
            .map(sale -> new AiDtos.SalePoint(
                sale.getSaleDate(),
                sale.getAgency().getCode(),
                sale.getProduct().getSku(),
                sale.getQuantity(),
                sale.getUnitPrice()
            ))
            .toList();

        if (salePoints.isEmpty()) {
            return new AiDtos.SalesAnomalyAnalysisResponse(0, 0, 0, List.of(), List.of());
        }

        AiDtos.SalesAnomalyResponse response = aiClientService.detectSalesAnomalies(
            new AiDtos.SalesAnomalyRequest(salePoints, DEFAULT_ZSCORE_THRESHOLD)
        );

        List<AiDtos.AiAlert> createdAlerts = response.results().stream()
            .filter(AiDtos.SalesAnomalyResult::anomaly)
            .map(this::createSalesAlert)
            .toList();

        return new AiDtos.SalesAnomalyAnalysisResponse(
            response.count(),
            response.anomalyCount(),
            createdAlerts.size(),
            createdAlerts,
            response.results()
        );
    }

    @Transactional
    public AiDtos.StockPredictionAnalysisResponse analyzeStockPredictions() {
        List<StockMovement> movements = stockMovementRepository.findAll().stream()
            .sorted(Comparator.comparing(StockMovement::getMovementDate))
            .toList();

        Map<StockKey, List<StockMovement>> groupedMovements = new LinkedHashMap<>();
        for (StockMovement movement : movements) {
            StockKey key = new StockKey(
                movement.getProduct().getSku(),
                movement.getAgency().getCode()
            );
            groupedMovements.computeIfAbsent(key, ignored -> new ArrayList<>()).add(movement);
        }

        List<AiDtos.StockPredictionResponse> predictions = new ArrayList<>();
        List<AiDtos.AiAlert> createdAlerts = new ArrayList<>();
        int skippedCount = 0;

        for (Map.Entry<StockKey, List<StockMovement>> entry : groupedMovements.entrySet()) {
            StockSnapshot snapshot = buildStockSnapshot(entry.getValue());
            if (snapshot.history().size() < 2) {
                skippedCount++;
                continue;
            }

            AiDtos.StockPredictionResponse prediction = aiClientService.predictStock(
                new AiDtos.StockPredictionRequest(
                    entry.getKey().productCode(),
                    entry.getKey().agencyCode(),
                    snapshot.currentStock(),
                    DEFAULT_REORDER_THRESHOLD,
                    snapshot.history()
                )
            );
            predictions.add(prediction);

            if (shouldCreateStockAlert(prediction.alertLevel())) {
                createdAlerts.add(createStockAlert(prediction));
            }
        }

        return new AiDtos.StockPredictionAnalysisResponse(
            predictions.size(),
            skippedCount,
            createdAlerts.size(),
            createdAlerts,
            predictions
        );
    }

    private StockSnapshot buildStockSnapshot(List<StockMovement> movements) {
        Map<LocalDate, Long> dailyDeltas = new LinkedHashMap<>();
        for (StockMovement movement : movements) {
            LocalDate day = movement.getMovementDate().toLocalDate();
            dailyDeltas.merge(day, signedQuantity(movement), Long::sum);
        }

        long runningStock = 0;
        List<AiDtos.StockHistoryPoint> history = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : dailyDeltas.entrySet()) {
            runningStock += entry.getValue();
            history.add(new AiDtos.StockHistoryPoint(entry.getKey(), Math.max(runningStock, 0)));
        }
        return new StockSnapshot(Math.max(runningStock, 0), history);
    }

    private long signedQuantity(StockMovement movement) {
        if (movement.getType() == StockMovementType.OUT) {
            return -movement.getQuantity();
        }
        return movement.getQuantity();
    }

    private AiDtos.AiAlert createSalesAlert(AiDtos.SalesAnomalyResult result) {
        AlertSeverity severity = toSeverity(result.alertLevel());
        String title = "Anomalie de vente detectee";
        String message = "Vente inhabituelle pour produit " + result.productCode()
            + " / agence " + result.agencyCode()
            + " le " + result.date()
            + " : quantite=" + result.quantity()
            + ", zScore=" + result.zScore();
        AlertDtos.AlertResponse alert = alertService.create(new AlertDtos.AlertRequest(severity, title, message));
        return new AiDtos.AiAlert(result.alertLevel(), severity, alert.id(), title, message);
    }

    private AiDtos.AiAlert createStockAlert(AiDtos.StockPredictionResponse prediction) {
        AlertSeverity severity = toSeverity(prediction.alertLevel());
        String title = "Risque de rupture de stock";
        String message = "Produit " + prediction.productCode()
            + " / agence " + prediction.agencyCode()
            + " : stock=" + prediction.currentStock()
            + ", jours avant rupture=" + Objects.toString(prediction.predictedDaysToStockout(), "inconnu")
            + ", recommandation=" + prediction.recommendation();
        AlertDtos.AlertResponse alert = alertService.create(new AlertDtos.AlertRequest(severity, title, message));
        return new AiDtos.AiAlert(prediction.alertLevel(), severity, alert.id(), title, message);
    }

    private boolean shouldCreateStockAlert(String alertLevel) {
        AlertSeverity severity = toSeverity(alertLevel);
        return severity == AlertSeverity.WARNING || severity == AlertSeverity.CRITICAL;
    }

    private AlertSeverity toSeverity(String alertLevel) {
        if ("critique".equalsIgnoreCase(alertLevel) || "critical".equalsIgnoreCase(alertLevel)) {
            return AlertSeverity.CRITICAL;
        }
        if ("moyen".equalsIgnoreCase(alertLevel) || "medium".equalsIgnoreCase(alertLevel)) {
            return AlertSeverity.WARNING;
        }
        return AlertSeverity.INFO;
    }

    private record StockKey(String productCode, String agencyCode) {
    }

    private record StockSnapshot(long currentStock, List<AiDtos.StockHistoryPoint> history) {
    }
}
