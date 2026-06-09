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
@Table(name = "journal_activites")
public class JournalActivite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JournalNiveau niveau = JournalNiveau.INFO;

    @Column(nullable = false)
    private String typeEvenement;

    @Column(nullable = false)
    private String module;

    @Column(nullable = false, length = 1200)
    private String message;

    @Column(nullable = false)
    private String utilisateur;

    @Column(nullable = false, updatable = false)
    private Instant dateEvenement = Instant.now();

    @Column(length = 2000)
    private String details;

    private String referenceObjet;

    public Long getId() {
        return id;
    }

    public JournalNiveau getNiveau() {
        return niveau;
    }

    public void setNiveau(JournalNiveau niveau) {
        this.niveau = niveau;
    }

    public String getTypeEvenement() {
        return typeEvenement;
    }

    public void setTypeEvenement(String typeEvenement) {
        this.typeEvenement = typeEvenement;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(String utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Instant getDateEvenement() {
        return dateEvenement;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getReferenceObjet() {
        return referenceObjet;
    }

    public void setReferenceObjet(String referenceObjet) {
        this.referenceObjet = referenceObjet;
    }
}
