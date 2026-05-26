package com.example.dataops.dto;

import com.example.dataops.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class UserDtos {
    private UserDtos() {
    }

    public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String fullName,
        @NotBlank @Size(min = 8) String password,
        UserRole role,
        Boolean active
    ) {
    }

    public record UpdateUserRequest(
        @Email String email,
        String fullName,
        String password,
        UserRole role,
        Boolean active
    ) {
    }

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
}

