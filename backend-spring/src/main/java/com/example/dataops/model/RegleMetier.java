package com.example.dataops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "regles_metier")
public class RegleMetier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegleMetierModule module;

    @Column(nullable = false)
    private String valeur;

    @Column(nullable = false)
    private String unite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeValeurRegle typeValeur;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(nullable = false)
    private Instant dateModification = Instant.now();

    @PrePersist
    @PreUpdate
    void touch() {
        dateModification = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RegleMetierModule getModule() {
        return module;
    }

    public void setModule(RegleMetierModule module) {
        this.module = module;
    }

    public String getValeur() {
        return valeur;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }

    public String getUnite() {
        return unite;
    }

    public void setUnite(String unite) {
        this.unite = unite;
    }

    public TypeValeurRegle getTypeValeur() {
        return typeValeur;
    }

    public void setTypeValeur(TypeValeurRegle typeValeur) {
        this.typeValeur = typeValeur;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public Instant getDateModification() {
        return dateModification;
    }
}
