package com.example.dataops.dto;

import com.example.dataops.model.UserRole;

import java.time.Instant;

public record UserResponse(
    Long id,
    String username,
    String email,
    String fullName,
    UserRole role,
    boolean active,
    Instant createdAt
) {
}

