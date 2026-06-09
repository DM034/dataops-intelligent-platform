package com.example.dataops.repository;

import com.example.dataops.model.JournalActivite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JournalActiviteRepository extends JpaRepository<JournalActivite, Long>, JpaSpecificationExecutor<JournalActivite> {
}
