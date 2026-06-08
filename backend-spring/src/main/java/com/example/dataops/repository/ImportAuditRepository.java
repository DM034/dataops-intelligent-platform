package com.example.dataops.repository;

import com.example.dataops.model.ImportAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportAuditRepository extends JpaRepository<ImportAudit, Long> {
    List<ImportAudit> findAllByOrderByImportDateDesc();
}
