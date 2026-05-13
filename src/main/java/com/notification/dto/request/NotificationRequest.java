package com.notification.dto.request;

import com.notification.enums.NotificationChannel;
import com.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record NotificationRequest(
        @NotNull Long recipientId,
        @NotNull NotificationType notificationType,
        @NotNull NotificationChannel channel,
        @NotBlank String eventId,
        String referenceId,
        LocalDateTime scheduledAt
) {}
