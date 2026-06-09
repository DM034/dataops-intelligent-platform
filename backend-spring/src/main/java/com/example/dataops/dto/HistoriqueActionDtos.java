package com.example.dataops.dto;

import com.example.dataops.model.HistoriqueModule;

import java.time.Instant;

public final class HistoriqueActionDtos {
    private HistoriqueActionDtos() {
    }

    public record HistoriqueActionResponse(
        Long id,
        String utilisateurId,
        String utilisateurNom,
        String action,
        HistoriqueModule module,
        String description,
        String ancienneValeur,
        String nouvelleValeur,
        Instant dateAction,
        String referenceObjet,
        String adresseIp
    ) {
    }
}
