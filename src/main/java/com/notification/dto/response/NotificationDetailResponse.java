package com.notification.dto.response;

import com.notification.entity.Notification;
import com.notification.enums.NotificationChannel;
import com.notification.enums.NotificationStatus;
import com.notification.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationDetailResponse(
        Long id,
        Long recipientId,
        NotificationType notificationType,
        NotificationChannel channel,
        NotificationStatus status,
        int retryCount,
        LocalDateTime nextRetryAt,
        boolean isRead,
        LocalDateTime readAt,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
    public static NotificationDetailResponse from(Notification notification) {
        return new NotificationDetailResponse(
                notification.getId(),
                notification.getRecipientId(),
                notification.getNotificationType(),
                notification.getChannel(),
                notification.getStatus(),
                notification.getRetryCount(),
                notification.getNextRetryAt(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getSentAt(),
                notification.getCreatedAt()
        );
    }
}
