package com.example.dataops.repository;

import com.example.dataops.model.Alerte;
import com.example.dataops.model.AlerteStatut;
import com.example.dataops.model.AlerteType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlerteRepository extends JpaRepository<Alerte, Long> {
    List<Alerte> findByStatutOrderByDateCreationDesc(AlerteStatut statut);

    List<Alerte> findAllByOrderByDateCreationDesc();

    Optional<Alerte> findByTypeAndReferenceObjetAndStatut(AlerteType type, String referenceObjet, AlerteStatut statut);
}
