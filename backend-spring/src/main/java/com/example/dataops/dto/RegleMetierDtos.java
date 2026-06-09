package com.example.dataops.dto;

import com.example.dataops.model.RegleMetierModule;
import com.example.dataops.model.TypeValeurRegle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class RegleMetierDtos {
    private RegleMetierDtos() {
    }

    public record RegleMetierResponse(
        Long id,
        String code,
        String libelle,
        String description,
        RegleMetierModule module,
        String valeur,
        String unite,
        TypeValeurRegle typeValeur,
        boolean actif,
        Instant dateModification
    ) {
    }

    public record UpdateRegleMetierRequest(
        @NotBlank String valeur,
        Boolean actif
    ) {
    }

    public record CreateRegleMetierRequest(
        @NotBlank String code,
        @NotBlank String libelle,
        @NotBlank String description,
        @NotNull RegleMetierModule module,
        @NotBlank String valeur,
        @NotBlank String unite,
        @NotNull TypeValeurRegle typeValeur,
        Boolean actif
    ) {
    }
}
