package com.example.dataops.mapper;

import com.example.dataops.dto.AgencyDtos;
import com.example.dataops.dto.AlertDtos;
import com.example.dataops.dto.BlockchainDtos;
import com.example.dataops.dto.ProductDtos;
import com.example.dataops.dto.SaleDtos;
import com.example.dataops.dto.StockDtos;
import com.example.dataops.dto.UserResponse;
import com.example.dataops.model.Agency;
import com.example.dataops.model.Alert;
import com.example.dataops.model.AppUser;
import com.example.dataops.model.BlockchainBlock;
import com.example.dataops.model.Product;
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
            block.getBlockIndex(),
            block.getTimestamp(),
            block.getAction(),
            block.getActor(),
            block.getPayload(),
            block.getPreviousHash(),
            block.getHash()
        );
    }

    public AlertDtos.AlertResponse toAlertResponse(Alert alert) {
        return new AlertDtos.AlertResponse(alert.getId(), alert.getSeverity(), alert.getTitle(), alert.getMessage(), alert.isResolved(), alert.getCreatedAt());
    }
}

