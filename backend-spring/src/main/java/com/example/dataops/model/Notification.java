package com.example.dataops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String utilisateurId;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, length = 1200)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationNiveau niveau;

    @Column(nullable = false)
    private boolean lu = false;

    @Column(nullable = false, updatable = false)
    private Instant dateCreation = Instant.now();

    private String lienAction;

    public Long getId() {
        return id;
    }

    public String getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(String utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationNiveau getNiveau() {
        return niveau;
    }

    public void setNiveau(NotificationNiveau niveau) {
        this.niveau = niveau;
    }

    public boolean isLu() {
        return lu;
    }

    public void setLu(boolean lu) {
        this.lu = lu;
    }

    public Instant getDateCreation() {
        return dateCreation;
    }

    public String getLienAction() {
        return lienAction;
    }

    public void setLienAction(String lienAction) {
        this.lienAction = lienAction;
    }
}
