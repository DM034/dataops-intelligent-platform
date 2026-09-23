package com.example.dataops.repository;

import com.example.dataops.model.StockMovement;
import com.example.dataops.model.StockMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    boolean existsByMovementDateAndAgency_CodeAndProduct_SkuAndQuantityAndType(LocalDateTime movementDate, String agencyCode, String productSku, Integer quantity, StockMovementType type);

    long countByImportJobId(String importJobId);

    void deleteByImportJobId(String importJobId);

    @Query("""
        select sm.product.name, sm.agency.name,
            coalesce(sum(case
                when sm.type = com.example.dataops.model.StockMovementType.IN then sm.quantity
                when sm.type = com.example.dataops.model.StockMovementType.OUT then -sm.quantity
                else sm.quantity
            end), 0)
        from StockMovement sm
        group by sm.product.name, sm.agency.name
        """)
    List<Object[]> stockLevels();

    @Query("""
        select sm.product, sm.agency,
            coalesce(sum(case
                when sm.type = com.example.dataops.model.StockMovementType.IN then sm.quantity
                when sm.type = com.example.dataops.model.StockMovementType.OUT then -sm.quantity
                else sm.quantity
            end), 0)
        from StockMovement sm
        group by sm.product, sm.agency
        """)
    List<Object[]> stockLevelsByEntity();
}
