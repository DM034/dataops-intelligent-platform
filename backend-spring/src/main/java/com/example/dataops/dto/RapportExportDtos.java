package com.example.dataops.dto;

import com.example.dataops.model.HistoriqueModule;

import java.time.LocalDate;

public final class RapportExportDtos {
    private RapportExportDtos() {
    }

    public enum TypeRapport {
        TABLEAU_BORD_GLOBAL,
        ALERTES_ACTIVES,
        SIMULATION_WHAT_IF,
        ACHATS_RECOMMANDES,
        NON_CONFORMITES,
        STOCKS_CRITIQUES
    }

    public record RapportExportFilter(
        TypeRapport typeRapport,
        LocalDate dateDebut,
        LocalDate dateFin,
        HistoriqueModule module,
        String statut
    ) {
    }
}
