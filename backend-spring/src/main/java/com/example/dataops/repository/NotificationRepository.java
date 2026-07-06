package com.example.dataops.repository;

import com.example.dataops.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUtilisateurIdOrderByDateCreationDesc(String utilisateurId);

    long countByUtilisateurIdAndLuFalse(String utilisateurId);
}
