package com.example.dataops.service;

import com.example.dataops.dto.NotificationDtos;
import com.example.dataops.exception.ResourceNotFoundException;
import com.example.dataops.model.Notification;
import com.example.dataops.model.NotificationNiveau;
import com.example.dataops.model.NotificationType;
import com.example.dataops.repository.NotificationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public NotificationDtos.NotificationListResponse findForCurrentUser() {
        String userId = currentUserId();
        return new NotificationDtos.NotificationListResponse(
            repository.countByUtilisateurIdAndLuFalse(userId),
            repository.findByUtilisateurIdOrderByDateCreationDesc(userId).stream()
                .map(this::toResponse)
                .toList()
        );
    }

    @Transactional
    public NotificationDtos.NotificationResponse create(NotificationDtos.CreateNotificationRequest request) {
        return toResponse(create(
            request.utilisateurId(),
            request.titre(),
            request.message(),
            request.type(),
            request.niveau(),
            request.lienAction()
        ));
    }

    @Transactional
    public Notification create(String utilisateurId, String titre, String message, NotificationType type, NotificationNiveau niveau, String lienAction) {
        Notification notification = new Notification();
        notification.setUtilisateurId(utilisateurId == null || utilisateurId.isBlank() ? currentUserId() : utilisateurId);
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setType(type);
        notification.setNiveau(niveau);
        notification.setLienAction(lienAction);
        return repository.save(notification);
    }

    @Transactional
    public NotificationDtos.NotificationResponse markAsRead(Long id) {
        Notification notification = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        notification.setLu(true);
        return toResponse(notification);
    }

    public String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || authentication.getName() == null ? "mock-user" : authentication.getName();
    }

    private NotificationDtos.NotificationResponse toResponse(Notification notification) {
        return new NotificationDtos.NotificationResponse(
            notification.getId(),
            notification.getUtilisateurId(),
            notification.getTitre(),
            notification.getMessage(),
            notification.getType(),
            notification.getNiveau(),
            notification.isLu(),
            notification.getDateCreation(),
            notification.getLienAction()
        );
    }
}
