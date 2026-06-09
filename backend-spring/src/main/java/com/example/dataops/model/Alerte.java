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
@Table(name = "alertes")
public class Alerte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlerteType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity niveauCriticite;

    @Column(nullable = false, length = 1200)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlerteSourceModule sourceModule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlerteStatut statut = AlerteStatut.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant dateCreation = Instant.now();

    private Instant dateResolution;

    @Column(nullable = false)
    private String referenceObjet;

    public Long getId() {
        return id;
    }

    public AlerteType getType() {
        return type;
    }

    public void setType(AlerteType type) {
        this.type = type;
    }

    public AlertSeverity getNiveauCriticite() {
        return niveauCriticite;
    }

    public void setNiveauCriticite(AlertSeverity niveauCriticite) {
        this.niveauCriticite = niveauCriticite;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AlerteSourceModule getSourceModule() {
        return sourceModule;
    }

    public void setSourceModule(AlerteSourceModule sourceModule) {
        this.sourceModule = sourceModule;
    }

    public AlerteStatut getStatut() {
        return statut;
    }

    public void setStatut(AlerteStatut statut) {
        this.statut = statut;
    }

    public Instant getDateCreation() {
        return dateCreation;
    }

    public Instant getDateResolution() {
        return dateResolution;
    }

    public void setDateResolution(Instant dateResolution) {
        this.dateResolution = dateResolution;
    }

    public String getReferenceObjet() {
        return referenceObjet;
    }

    public void setReferenceObjet(String referenceObjet) {
        this.referenceObjet = referenceObjet;
    }
}
