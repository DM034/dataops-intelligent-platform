package com.example.dataops.model;

public enum RecommendationStatus {
    PROPOSEE,
    VALIDEE,
    REJETEE,
    EN_ATTENTE,

    @Deprecated
    NEW,
    @Deprecated
    IN_PROGRESS,
    @Deprecated
    DONE,
    @Deprecated
    IGNORED
}
