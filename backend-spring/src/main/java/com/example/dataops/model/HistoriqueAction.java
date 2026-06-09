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
@Table(name = "historique_actions")
public class HistoriqueAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String utilisateurId;

    @Column(nullable = false)
    private String utilisateurNom;

    @Column(nullable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HistoriqueModule module;

    @Column(nullable = false, length = 1200)
    private String description;

    @Column(length = 1200)
    private String ancienneValeur;

    @Column(length = 1200)
    private String nouvelleValeur;

    @Column(nullable = false, updatable = false)
    private Instant dateAction = Instant.now();

    private String referenceObjet;

    private String adresseIp;

    public Long getId() {
        return id;
    }

    public String getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(String utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public String getUtilisateurNom() {
        return utilisateurNom;
    }

    public void setUtilisateurNom(String utilisateurNom) {
        this.utilisateurNom = utilisateurNom;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public HistoriqueModule getModule() {
        return module;
    }

    public void setModule(HistoriqueModule module) {
        this.module = module;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAncienneValeur() {
        return ancienneValeur;
    }

    public void setAncienneValeur(String ancienneValeur) {
        this.ancienneValeur = ancienneValeur;
    }

    public String getNouvelleValeur() {
        return nouvelleValeur;
    }

    public void setNouvelleValeur(String nouvelleValeur) {
        this.nouvelleValeur = nouvelleValeur;
    }

    public Instant getDateAction() {
        return dateAction;
    }

    public String getReferenceObjet() {
        return referenceObjet;
    }

    public void setReferenceObjet(String referenceObjet) {
        this.referenceObjet = referenceObjet;
    }

    public String getAdresseIp() {
        return adresseIp;
    }

    public void setAdresseIp(String adresseIp) {
        this.adresseIp = adresseIp;
    }
}
