package com.notification.controller;

import com.notification.dto.request.NotificationRequest;
import com.notification.dto.response.NotificationCreateResponse;
import com.notification.dto.response.NotificationDetailResponse;
import com.notification.dto.response.NotificationListResponse;
import com.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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

    @GetMapping
    public ResponseEntity<List<NotificationListResponse>> getList(
            @RequestParam Long recipientId,
            @RequestParam(required = false) Boolean isRead) {
        return ResponseEntity.ok(notifService.getList(recipientId, isRead));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDetailResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(notifService.getDetail(id));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retry(@PathVariable Long id) {
        notifService.retry(id);
        return ResponseEntity.ok().build();
    }
}
