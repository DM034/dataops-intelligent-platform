package com.example.dataops.mapper;

import com.example.dataops.dto.AgencyDtos;
import com.example.dataops.dto.AlertDtos;
import com.example.dataops.dto.BlockchainDtos;
import com.example.dataops.dto.DataGovernanceDtos;
import com.example.dataops.dto.ProductDtos;
import com.example.dataops.dto.RecommendationDtos;
import com.example.dataops.dto.SaleDtos;
import com.example.dataops.dto.StockDtos;
import com.example.dataops.dto.UserResponse;
import com.example.dataops.model.Agency;
import com.example.dataops.model.Alert;
import com.example.dataops.model.AppUser;
import com.example.dataops.model.BlockchainBlock;
import com.example.dataops.model.DataCatalog;
import com.example.dataops.model.DataLineage;
import com.example.dataops.model.DataQualityReport;
import com.example.dataops.model.ImportAudit;
import com.example.dataops.model.Product;
import com.example.dataops.model.Recommendation;
import com.example.dataops.model.Sale;
import com.example.dataops.model.StockMovement;
import org.springframework.stereotype.Component;

@Component
public class DataopsMapper {
    public UserResponse toUserResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getRole(), user.isActive(), user.getCreatedAt());
    }

    public AgencyDtos.AgencyResponse toAgencyResponse(Agency agency) {
        return new AgencyDtos.AgencyResponse(agency.getId(), agency.getCode(), agency.getName(), agency.getCity(), agency.isActive(), agency.getCreatedAt());
    }

    public ProductDtos.ProductResponse toProductResponse(Product product) {
        return new ProductDtos.ProductResponse(product.getId(), product.getSku(), product.getName(), product.getCategory(), product.getUnitPrice(), product.isActive(), product.getCreatedAt());
    }

    public SaleDtos.SaleResponse toSaleResponse(Sale sale) {
        return new SaleDtos.SaleResponse(
            sale.getId(),
            sale.getAgency().getId(),
            sale.getAgency().getName(),
            sale.getProduct().getId(),
            sale.getProduct().getName(),
            sale.getQuantity(),
            sale.getUnitPrice(),
            sale.getTotalAmount(),
            sale.getSaleDate(),
            sale.getReference()
        );
    }

    public StockDtos.StockMovementResponse toStockMovementResponse(StockMovement movement) {
        return new StockDtos.StockMovementResponse(
            movement.getId(),
            movement.getAgency().getId(),
            movement.getAgency().getName(),
            movement.getProduct().getId(),
            movement.getProduct().getName(),
            movement.getType(),
            movement.getQuantity(),
            movement.getMovementDate(),
            movement.getReason()
        );
    }

    public BlockchainDtos.BlockchainBlockResponse toBlockchainBlockResponse(BlockchainBlock block) {
        return new BlockchainDtos.BlockchainBlockResponse(
            block.getId(),
            block.getTimestamp(),
            block.getAction(),
            block.getEntityType(),
            block.getEntityId(),
            block.getUserId(),
            block.getDataHash(),
            block.getPreviousHash(),
            block.getCurrentHash()
        );
    }

    public AlertDtos.AlertResponse toAlertResponse(Alert alert) {
        return new AlertDtos.AlertResponse(alert.getId(), alert.getSeverity(), alert.getTitle(), alert.getMessage(), alert.isResolved(), alert.getCreatedAt());
    }

    public DataGovernanceDtos.DataQualityReportResponse toDataQualityReportResponse(DataQualityReport report) {
        return new DataGovernanceDtos.DataQualityReportResponse(
            report.getId(),
            report.getImportFileId(),
            report.getSourceName(),
            report.getTotalRows(),
            report.getValidRows(),
            report.getErrorRows(),
            report.getDuplicateRecords(),
            report.getCompletenessRate(),
            report.getValidityRate(),
            report.getUniquenessRate(),
            report.getConsistencyRate(),
            report.getGlobalScore(),
            report.getTotalRecords(),
            report.getValidRecords(),
            report.getInvalidRecords(),
            report.getQualityScore(),
            report.getCreatedAt()
        );
    }

    public DataGovernanceDtos.DataLineageResponse toDataLineageResponse(DataLineage lineage) {
        return new DataGovernanceDtos.DataLineageResponse(
            lineage.getId(),
            lineage.getSourceName(),
            lineage.getSource(),
            lineage.getSourceType(),
            lineage.getImportDate(),
            lineage.getValidationDate(),
            lineage.getTransformationDate(),
            lineage.getStorageDate(),
            lineage.getDashboardDate(),
            lineage.getValidationStep(),
            lineage.getTransformationStep(),
            lineage.getStorageStep(),
            lineage.getDashboardStep(),
            lineage.getStatus()
        );
    }

    public DataGovernanceDtos.DataCatalogResponse toDataCatalogResponse(DataCatalog catalog) {
        return new DataGovernanceDtos.DataCatalogResponse(
            catalog.getId(),
            catalog.getName(),
            catalog.getSourceType(),
            catalog.getDescription(),
            catalog.getOwner(),
            catalog.getRefreshFrequency(),
            catalog.getCreatedAt(),
            catalog.getUpdatedAt()
        );
    }

    public DataGovernanceDtos.ImportAuditResponse toImportAuditResponse(ImportAudit audit) {
        return new DataGovernanceDtos.ImportAuditResponse(
            audit.getId(),
            audit.getFileName(),
            audit.getImportedBy(),
            audit.getImportDate(),
            audit.getTotalRows(),
            audit.getSuccessRows(),
            audit.getFailedRows(),
            audit.getStatus()
        );
    }

    public RecommendationDtos.RecommendationResponse toRecommendationResponse(Recommendation recommendation) {
        return new RecommendationDtos.RecommendationResponse(
            recommendation.getId(),
            recommendation.getType(),
            recommendation.getModuleSource(),
            recommendation.getSeverity(),
            recommendation.getPriority(),
            recommendation.getMessage(),
            recommendation.getDescription() == null || recommendation.getDescription().isBlank() ? recommendation.getMessage() : recommendation.getDescription(),
            recommendation.getSuggestedAction(),
            recommendation.getEstimatedImpact() == null || recommendation.getEstimatedImpact().isBlank() ? "Impact a qualifier" : recommendation.getEstimatedImpact(),
            recommendation.getAgency() == null ? null : recommendation.getAgency().getId(),
            recommendation.getAgency() == null ? null : recommendation.getAgency().getName(),
            recommendation.getProduct() == null ? null : recommendation.getProduct().getId(),
            recommendation.getProduct() == null ? null : recommendation.getProduct().getName(),
            recommendation.getRelatedAlert() == null ? null : recommendation.getRelatedAlert().getId(),
            recommendation.getCreatedAt(),
            recommendation.getStatus()
        );
    }
}
