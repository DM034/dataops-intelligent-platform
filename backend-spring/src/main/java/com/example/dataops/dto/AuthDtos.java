package com.example.dataops.dto;

import com.example.dataops.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String fullName,
        @NotBlank @Size(min = 8) String password,
        UserRole role
    ) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, UserResponse user) {
    }
}

