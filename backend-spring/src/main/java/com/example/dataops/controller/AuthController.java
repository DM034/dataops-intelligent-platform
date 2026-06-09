package com.example.dataops.controller;

import com.example.dataops.dto.AuthDtos;
import com.example.dataops.model.JournalNiveau;
import com.example.dataops.service.AuthService;
import com.example.dataops.service.JournalActiviteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JournalActiviteService journalActiviteService;

    public AuthController(AuthService authService, JournalActiviteService journalActiviteService) {
        this.authService = authService;
        this.journalActiviteService = journalActiviteService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.TokenResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.TokenResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        String user = authentication == null ? "anonymous" : authentication.getName();
        journalActiviteService.journaliser(JournalNiveau.INFO, "DECONNEXION", "AUTH", "Deconnexion utilisateur", user, null, user);
        return ResponseEntity.noContent().build();
    }
}
