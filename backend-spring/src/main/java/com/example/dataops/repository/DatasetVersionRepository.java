package com.example.dataops.repository;

import com.example.dataops.model.DatasetVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasetVersionRepository extends JpaRepository<DatasetVersion, Long> {
    List<DatasetVersion> findAllByOrderByCreatedAtDesc();

    List<DatasetVersion> findTop5ByDatasetNameOrderByCreatedAtDesc(String datasetName);
}
