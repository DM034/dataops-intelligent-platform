package com.example.dataops.repository;

import com.example.dataops.model.RegleMetier;
import com.example.dataops.model.RegleMetierModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegleMetierRepository extends JpaRepository<RegleMetier, Long> {
    Optional<RegleMetier> findByCode(String code);

    boolean existsByCode(String code);

    List<RegleMetier> findByModuleOrderByCode(RegleMetierModule module);

    List<RegleMetier> findAllByOrderByModuleAscCodeAsc();
}
