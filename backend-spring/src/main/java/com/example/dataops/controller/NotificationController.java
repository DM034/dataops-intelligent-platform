package com.example.dataops.controller;

import com.example.dataops.dto.NotificationDtos;
import com.example.dataops.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public NotificationDtos.NotificationListResponse notifications() {
        return service.findForCurrentUser();
    }

    @PatchMapping("/{id}/read")
    public NotificationDtos.NotificationResponse markAsRead(@PathVariable Long id) {
        return service.markAsRead(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationDtos.NotificationResponse create(@Valid @RequestBody NotificationDtos.CreateNotificationRequest request) {
        return service.create(request);
    }
}
