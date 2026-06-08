package com.example.dataops.repository;

import com.example.dataops.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findAllByOrderByCreatedAtDesc();
}
