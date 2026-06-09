package com.example.dataops.exception;

import com.example.dataops.model.JournalNiveau;
import com.example.dataops.service.JournalActiviteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final JournalActiviteService journalActiviteService;

    public GlobalExceptionHandler(JournalActiviteService journalActiviteService) {
        this.journalActiviteService = journalActiviteService;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException exception) {
        journalActiviteService.journaliser(JournalNiveau.WARNING, "ERREUR_SYSTEME", "SYSTEME", "Ressource introuvable", exception.getMessage(), null);
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> business(BusinessException exception) {
        journalActiviteService.journaliser(JournalNiveau.WARNING, "ERREUR_SYSTEME", "SYSTEME", "Erreur metier", exception.getMessage(), null);
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .orElse("Invalid request");
        journalActiviteService.journaliser(JournalNiveau.WARNING, "ERREUR_SYSTEME", "SYSTEME", "Erreur de validation", message, null);
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> system(Exception exception) {
        journalActiviteService.journaliser(JournalNiveau.ERROR, "ERREUR_SYSTEME", "SYSTEME", "Erreur systeme non geree", exception.getClass().getSimpleName() + ": " + exception.getMessage(), null);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "timestamp", Instant.now(),
            "status", status.value(),
            "error", status.getReasonPhrase(),
            "message", message
        ));
    }
}
