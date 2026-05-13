package com.notification.dto.response;

import com.notification.entity.Notification;
import com.notification.enums.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationCreateResponse(
        Long id,
        NotificationStatus status,
        LocalDateTime createdAt
) {
    public static NotificationCreateResponse from(Notification notification) {
        return new NotificationCreateResponse(
                notification.getId(),
                notification.getStatus(),
                notification.getCreatedAt()
        );
    }
}
