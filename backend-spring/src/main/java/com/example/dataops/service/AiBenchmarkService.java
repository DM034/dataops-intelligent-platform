package com.example.dataops.service;

import com.example.dataops.dto.AiDtos;
import com.example.dataops.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        try {
            return aiClientService.benchmarkAnomalies(sales);
        } catch (RestClientException ex) {
            return benchmarkLocally(sales);
        }
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

    private AiDtos.BenchmarkAnomalyResponse benchmarkLocally(List<AiDtos.BenchmarkSalePoint> sales) {
        AiDtos.BenchmarkMethodResult zscore = zscore(sales);
        AiDtos.BenchmarkMethodResult iqr = iqr(sales);
        AiDtos.BenchmarkMethodResult movingAverage = movingAverage(sales);
        return new AiDtos.BenchmarkAnomalyResponse(
            zscore,
            iqr,
            movingAverage,
            recommend(zscore, iqr, movingAverage)
        );
    }

    private AiDtos.BenchmarkMethodResult zscore(List<AiDtos.BenchmarkSalePoint> sales) {
        long start = System.nanoTime();
        if (sales.isEmpty()) {
            return result(List.of(), start);
        }

        double mean = sales.stream().mapToDouble(this::amount).average().orElse(0);
        double variance = sales.stream()
            .mapToDouble(point -> Math.pow(amount(point) - mean, 2))
            .average()
            .orElse(0);
        double std = Math.sqrt(variance);
        if (std == 0) {
            return result(List.of(), start);
        }

        List<AiDtos.BenchmarkAnomaly> anomalies = sales.stream()
            .map(point -> anomaly(point, Math.abs((amount(point) - mean) / std), "Ecart statistique Z-score"))
            .filter(anomaly -> anomaly.score() >= 2.5)
            .sorted(Comparator.comparingDouble(AiDtos.BenchmarkAnomaly::score).reversed())
            .limit(25)
            .toList();
        return result(anomalies, start);
    }

    private AiDtos.BenchmarkMethodResult iqr(List<AiDtos.BenchmarkSalePoint> sales) {
        long start = System.nanoTime();
        if (sales.size() < 4) {
            return result(List.of(), start);
        }

        List<Double> amounts = sales.stream().map(this::amount).sorted().toList();
        double q1 = percentile(amounts, 0.25);
        double q3 = percentile(amounts, 0.75);
        double iqr = q3 - q1;
        double lower = q1 - 1.5 * iqr;
        double upper = q3 + 1.5 * iqr;

        List<AiDtos.BenchmarkAnomaly> anomalies = sales.stream()
            .filter(point -> amount(point) < lower || amount(point) > upper)
            .map(point -> anomaly(point, Math.max(Math.abs(amount(point) - q1), Math.abs(amount(point) - q3)), "Valeur hors intervalle IQR"))
            .sorted(Comparator.comparingDouble(AiDtos.BenchmarkAnomaly::score).reversed())
            .limit(25)
            .toList();
        return result(anomalies, start);
    }

    private AiDtos.BenchmarkMethodResult movingAverage(List<AiDtos.BenchmarkSalePoint> sales) {
        long start = System.nanoTime();
        Map<String, List<AiDtos.BenchmarkSalePoint>> grouped = new HashMap<>();
        for (AiDtos.BenchmarkSalePoint point : sales) {
            grouped.computeIfAbsent(point.agencyCode() + "|" + point.productCode(), ignored -> new ArrayList<>()).add(point);
        }

        List<AiDtos.BenchmarkAnomaly> anomalies = new ArrayList<>();
        for (List<AiDtos.BenchmarkSalePoint> series : grouped.values()) {
            series.sort(Comparator.comparing(AiDtos.BenchmarkSalePoint::date));
            for (int index = 3; index < series.size(); index++) {
                int from = Math.max(0, index - 7);
                double average = series.subList(from, index).stream().mapToDouble(this::amount).average().orElse(0);
                double current = amount(series.get(index));
                if (average > 0 && (current > average * 1.8 || current < average * 0.35)) {
                    anomalies.add(anomaly(series.get(index), Math.abs(current - average), "Ecart par rapport a la moyenne mobile 7 jours"));
                }
            }
        }

        anomalies.sort(Comparator.comparingDouble(AiDtos.BenchmarkAnomaly::score).reversed());
        return result(anomalies.stream().limit(25).toList(), start);
    }

    private AiDtos.BenchmarkMethodResult result(List<AiDtos.BenchmarkAnomaly> anomalies, long start) {
        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        return new AiDtos.BenchmarkMethodResult(anomalies, Math.round(elapsedMs * 1000.0) / 1000.0, anomalies.size());
    }

    private AiDtos.BenchmarkAnomaly anomaly(AiDtos.BenchmarkSalePoint point, double score, String reason) {
        return new AiDtos.BenchmarkAnomaly(
            point.date(),
            point.agencyCode(),
            point.productCode(),
            point.quantity(),
            point.amount(),
            Math.round(score * 10000.0) / 10000.0,
            reason
        );
    }

    private double amount(AiDtos.BenchmarkSalePoint point) {
        return point.amount().doubleValue();
    }

    private double percentile(List<Double> values, double percentile) {
        if (values.isEmpty()) {
            return 0;
        }
        int index = (int) Math.floor(percentile * (values.size() - 1));
        return values.get(index);
    }

    private String recommend(
        AiDtos.BenchmarkMethodResult zscore,
        AiDtos.BenchmarkMethodResult iqr,
        AiDtos.BenchmarkMethodResult movingAverage
    ) {
        if (zscore.anomalyCount() <= iqr.anomalyCount() && zscore.anomalyCount() <= movingAverage.anomalyCount()) {
            return "Z_SCORE";
        }
        if (iqr.anomalyCount() <= movingAverage.anomalyCount()) {
            return "IQR";
        }
        return "MOVING_AVERAGE";
    }

}
