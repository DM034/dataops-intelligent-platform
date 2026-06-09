package com.example.dataops.model;

public enum UserRole {
    ADMIN,
    DIRECTION,
    RESPONSABLE_PRODUCTION,
    RESPONSABLE_STOCK,
    RESPONSABLE_QUALITE,
    RESPONSABLE_ACHAT,
    UTILISATEUR_SIMPLE,

    @Deprecated
    MANAGER,
    @Deprecated
    ANALYST
}
