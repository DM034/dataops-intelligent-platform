package com.example.dataops.repository;

import com.example.dataops.model.HistoriqueAction;
import com.example.dataops.model.HistoriqueModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface HistoriqueActionRepository extends JpaRepository<HistoriqueAction, Long>, JpaSpecificationExecutor<HistoriqueAction> {
    List<HistoriqueAction> findAllByOrderByDateActionDesc();

    List<HistoriqueAction> findTop50ByOrderByDateActionDesc();
}
