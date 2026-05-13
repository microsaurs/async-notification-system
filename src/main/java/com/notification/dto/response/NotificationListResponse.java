package com.notification.dto.response;

import com.notification.entity.Notification;
import com.notification.enums.NotificationChannel;
import com.notification.enums.NotificationStatus;
import com.notification.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationListResponse(
        Long id,
        NotificationType notificationType,
        NotificationChannel channel,
        NotificationStatus status,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationListResponse from(Notification notification) {
        return new NotificationListResponse(
                notification.getId(),
                notification.getNotificationType(),
                notification.getChannel(),
                notification.getStatus(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
