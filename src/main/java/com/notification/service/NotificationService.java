package com.notification.service;

import com.notification.dto.request.NotificationRequest;
import com.notification.dto.response.NotificationCreateResponse;
import com.notification.dto.response.NotificationDetailResponse;
import com.notification.dto.response.NotificationListResponse;
import com.notification.entity.Notification;
import com.notification.exception.NotificationNotFoundException;
import com.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notifRepo;

    @Value("${notification.retry.max-count}")
    private int maxRetryCount;

    public NotificationService(NotificationRepository notifRepo) {
        this.notifRepo = notifRepo;
    }

    @Transactional
    public NotificationCreateResponse register(NotificationRequest req) {
        String idempotencyKey = buildIdempotencyKey(req);

        return notifRepo.findByIdempotencyKey(idempotencyKey)
                .map(NotificationCreateResponse::from)
                .orElseGet(() -> {
                    Notification notif = Notification.builder()
                            .recipientId(req.recipientId())
                            .notificationType(req.notificationType())
                            .channel(req.channel())
                            .eventId(req.eventId())
                            .referenceId(req.referenceId())
                            .idempotencyKey(idempotencyKey)
                            .maxRetryCount(maxRetryCount)
                            .scheduledAt(req.scheduledAt())
                            .build();
                    notifRepo.save(notif);
                    return NotificationCreateResponse.from(notif);
                });
    }

    @Transactional(readOnly = true)
    public List<NotificationListResponse> getList(Long recipientId, Boolean isRead) {
        List<Notification> notifications = (isRead != null)
                ? notifRepo.findByRecipientIdAndIsReadOrderByCreatedAtDesc(recipientId, isRead)
                : notifRepo.findByRecipientIdOrderByCreatedAtDesc(recipientId);
        return notifications.stream().map(NotificationListResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse getDetail(Long id) {
        Notification notif = notifRepo.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        return NotificationDetailResponse.from(notif);
    }

    private String buildIdempotencyKey(NotificationRequest req) {
        return req.notificationType() + ":" + req.channel() + ":" + req.recipientId() + ":" + req.eventId();
    }
}
