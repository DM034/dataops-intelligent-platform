package com.example.dataops.repository;

import com.example.dataops.model.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportJobRepository extends JpaRepository<ImportJob, String> {
    List<ImportJob> findAllByOrderByStartedAtDesc();
}
