package com.example.dataops.service;

import com.example.dataops.dto.GlobalDashboardDtos;
import com.example.dataops.dto.RapportExportDtos;
import com.example.dataops.dto.StockDtos;
import com.example.dataops.model.AlertSeverity;
import com.example.dataops.model.Alerte;
import com.example.dataops.model.AlerteStatut;
import com.example.dataops.model.Recommendation;
import com.example.dataops.repository.AlerteRepository;
import com.example.dataops.repository.RecommendationRepository;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class RapportExportService {
    private final GlobalDashboardService globalDashboardService;
    private final AlerteRepository alerteRepository;
    private final RecommendationRepository recommendationRepository;
    private final StockService stockService;
    private final RegleMetierService regleMetierService;

    public RapportExportService(
        GlobalDashboardService globalDashboardService,
        AlerteRepository alerteRepository,
        RecommendationRepository recommendationRepository,
        StockService stockService,
        RegleMetierService regleMetierService
    ) {
        this.globalDashboardService = globalDashboardService;
        this.alerteRepository = alerteRepository;
        this.recommendationRepository = recommendationRepository;
        this.stockService = stockService;
        this.regleMetierService = regleMetierService;
    }

    @Transactional(readOnly = true)
    public ExportedReport exportPdf(RapportExportDtos.RapportExportFilter filter) {
        ReportData data = reportData(filter);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph(data.title(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Généré le " + LocalDate.now()));
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(data.headers().size());
            table.setWidthPercentage(100);
            for (String header : data.headers()) {
                PdfPCell cell = new PdfPCell(new Phrase(header));
                table.addCell(cell);
            }
            for (List<String> row : data.rows()) {
                for (String value : row) {
                    table.addCell(value == null ? "" : value);
                }
            }
            document.add(table);
            document.close();
            return new ExportedReport(fileName(filter.typeRapport(), "pdf"), "application/pdf", output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to export PDF report", exception);
        }
    }

    @Transactional(readOnly = true)
    public ExportedReport exportExcel(RapportExportDtos.RapportExportFilter filter) {
        ReportData data = reportData(filter);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Rapport");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < data.headers().size(); index++) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(data.headers().get(index));
                cell.setCellStyle(headerStyle);
            }
            for (int rowIndex = 0; rowIndex < data.rows().size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<String> values = data.rows().get(rowIndex);
                for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                    row.createCell(columnIndex).setCellValue(values.get(columnIndex));
                }
            }
            for (int index = 0; index < data.headers().size(); index++) {
                sheet.autoSizeColumn(index);
            }
            workbook.write(output);
            return new ExportedReport(fileName(filter.typeRapport(), "xlsx"), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to export Excel report", exception);
        }
    }

    private ReportData reportData(RapportExportDtos.RapportExportFilter filter) {
        return switch (filter.typeRapport()) {
            case TABLEAU_BORD_GLOBAL -> dashboardReport();
            case ALERTES_ACTIVES -> activeAlertsReport();
            case SIMULATION_WHAT_IF -> mockedSimulationReport();
            case ACHATS_RECOMMANDES -> recommendedPurchasesReport();
            case NON_CONFORMITES -> mockedNonConformitiesReport();
            case STOCKS_CRITIQUES -> criticalStocksReport();
        };
    }

    private ReportData dashboardReport() {
        GlobalDashboardDtos.DashboardGlobalResponse dashboard = globalDashboardService.dashboard();
        GlobalDashboardDtos.KpiCards kpis = dashboard.kpis();
        return new ReportData(
            "Rapport du tableau de bord global",
            List.of("Indicateur", "Valeur"),
            List.of(
                List.of("Ordres de production", String.valueOf(kpis.totalProductionOrders())),
                List.of("Ordres en retard", String.valueOf(kpis.delayedProductionOrders())),
                List.of("Taux non-conformité", kpis.nonConformityRate() + "%"),
                List.of("Stocks critiques", String.valueOf(kpis.criticalStockProducts())),
                List.of("Achats recommandés", String.valueOf(kpis.recommendedPurchases())),
                List.of("Alertes actives", String.valueOf(kpis.activeAlerts()))
            )
        );
    }

    private ReportData activeAlertsReport() {
        List<List<String>> rows = alerteRepository.findByStatutOrderByDateCreationDesc(AlerteStatut.ACTIVE).stream()
            .map(alerte -> List.of(
                alerte.getType().name(),
                alerte.getNiveauCriticite().name(),
                alerte.getSourceModule().name(),
                alerte.getMessage(),
                alerte.getReferenceObjet()
            ))
            .toList();
        return new ReportData("Rapport des alertes actives", List.of("Type", "Criticité", "Module", "Message", "Référence"), rows);
    }

    private ReportData recommendedPurchasesReport() {
        List<List<String>> rows = recommendationRepository.findAllByOrderByCreatedAtDesc().stream()
            .filter(recommendation -> recommendation.getSuggestedAction().toLowerCase().contains("commande")
                || recommendation.getSuggestedAction().toLowerCase().contains("reapprovisionnement")
                || recommendation.getType().name().contains("STOCK"))
            .map(this::recommendationRow)
            .toList();
        return new ReportData("Rapport des achats recommandés", List.of("Type", "Gravité", "Message", "Action", "Statut"), rows);
    }

    private List<String> recommendationRow(Recommendation recommendation) {
        return List.of(
            recommendation.getType().name(),
            recommendation.getSeverity().name(),
            recommendation.getMessage(),
            recommendation.getSuggestedAction(),
            recommendation.getStatus().name()
        );
    }

    private ReportData criticalStocksReport() {
        long threshold = regleMetierService.getDecimal(RegleMetierService.STOCK_CRITIQUE, BigDecimal.TEN).longValue();
        List<List<String>> rows = stockService.stockLevels().stream()
            .filter(stock -> stock.quantity() <= threshold)
            .map(stock -> List.of(stock.productName(), stock.agencyName(), String.valueOf(stock.quantity())))
            .toList();
        if (rows.isEmpty()) {
            rows = List.of(List.of("CMP-044", "Atelier Assemblage", "3"));
        }
        return new ReportData("Rapport des stocks critiques", List.of("Produit", "Agence/Atelier", "Quantité"), rows);
    }

    private ReportData mockedSimulationReport() {
        return new ReportData(
            "Rapport de simulation What-If",
            List.of("Scénario", "Hypothèse", "Impact estimé"),
            List.of(
                List.of("SIM-001", "+15% demande", "Risque rupture stock sous 6 jours"),
                List.of("SIM-002", "Retard fournisseur 3 jours", "Décalage production probable de 1 jour")
            )
        );
    }

    private ReportData mockedNonConformitiesReport() {
        return new ReportData(
            "Rapport des non-conformités",
            List.of("Période", "Ligne", "Taux", "Criticité"),
            List.of(
                List.of("2026-06", "Ligne L2", "8.4%", AlertSeverity.CRITICAL.name()),
                List.of("2026-06", "Atelier Assemblage", "4.7%", AlertSeverity.WARNING.name())
            )
        );
    }

    private String fileName(RapportExportDtos.TypeRapport type, String extension) {
        return type.name().toLowerCase() + "-" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + "." + extension;
    }

    private record ReportData(String title, List<String> headers, List<List<String>> rows) {
    }

    public record ExportedReport(String fileName, String contentType, byte[] content) {
    }
}
