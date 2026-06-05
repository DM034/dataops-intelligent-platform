package com.example.dataops.repository;

import com.example.dataops.model.DataQualityReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataQualityReportRepository extends JpaRepository<DataQualityReport, Long> {
}
