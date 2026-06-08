package com.example.dataops.repository;

import com.example.dataops.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
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
