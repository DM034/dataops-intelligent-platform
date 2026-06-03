package com.example.dataops.repository;

import com.example.dataops.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findBySaleDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("select coalesce(sum(s.totalAmount), 0) from Sale s")
    BigDecimal totalRevenue();

    @Query("select coalesce(sum(s.quantity), 0) from Sale s")
    Long totalUnitsSold();

    @Query("""
        select s.agency.name, coalesce(sum(s.totalAmount), 0)
        from Sale s
        group by s.agency.name
        order by coalesce(sum(s.totalAmount), 0) desc
        """)
    List<Object[]> revenueByAgency();

    @Query("""
        select s.product.name, coalesce(sum(s.quantity), 0)
        from Sale s
        group by s.product.name
        order by coalesce(sum(s.quantity), 0) desc
        """)
    List<Object[]> unitsByProduct();

    @Query("""
        select s.saleDate, coalesce(sum(s.totalAmount), 0), count(s), coalesce(sum(s.quantity), 0)
        from Sale s
        group by s.saleDate
        order by s.saleDate
        """)
    List<Object[]> dailySales();
}
