package com.example.dataops.repository;

import com.example.dataops.model.DataCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataCatalogRepository extends JpaRepository<DataCatalog, Long> {
    boolean existsByName(String name);
}
