package com.notification.controller;

import com.notification.dto.request.NotificationRequest;
import com.notification.dto.response.NotificationCreateResponse;
import com.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifService;

    public NotificationController(NotificationService notifService) {
        this.notifService = notifService;
    }

    @PostMapping
    public ResponseEntity<NotificationCreateResponse> register(@Valid @RequestBody NotificationRequest req) {
        return ResponseEntity.ok(notifService.register(req));
    }
}
