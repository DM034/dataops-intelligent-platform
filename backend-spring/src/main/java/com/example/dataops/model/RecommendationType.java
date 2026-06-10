package com.example.dataops.model;

public enum RecommendationType {
    OPTIMISATION_PLANNING,
    SIMULATION_WHAT_IF,
    PREDICTION_NON_CONFORMITE,
    OPTIMISATION_ACHATS,
    ALERTE_INTELLIGENTE,

    @Deprecated
    AI_ALERT,
    @Deprecated
    STOCKOUT_RISK,
    @Deprecated
    CRITICAL_STOCK,
    @Deprecated
    SALES_ANOMALY,
    @Deprecated
    DORMANT_STOCK
}
