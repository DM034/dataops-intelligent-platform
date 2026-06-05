package com.example.dataops.repository;

import com.example.dataops.model.DataLineage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataLineageRepository extends JpaRepository<DataLineage, Long> {
}
