package com.notification.entity;

import com.notification.enums.NotificationChannel;
import com.notification.enums.NotificationStatus;
import com.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "notification",
    indexes = {
        @Index(name = "idx_recipient_id", columnList = "recipient_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true)
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private NotificationChannel channel;

    // 알림을 발생시킨 이벤트 ID (중복 발송 방지 기준)
    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "max_retry_count", nullable = false)
    private int maxRetryCount;

    // 다음 재시도 예정 시각
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    // 처리 시작 시각 - stuck 감지에 사용
    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // 예약 발송 시각
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Notification(Long recipientId, NotificationType notificationType,
                        NotificationChannel channel, String eventId,
                        String referenceId, String idempotencyKey,
                        int maxRetryCount, LocalDateTime scheduledAt) {
        this.recipientId = recipientId;
        this.notificationType = notificationType;
        this.channel = channel;
        this.eventId = eventId;
        this.referenceId = referenceId;
        this.idempotencyKey = idempotencyKey;
        this.maxRetryCount = maxRetryCount;
        this.scheduledAt = scheduledAt;
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.isRead = false;
    }

    public void startSending() {
        this.status = NotificationStatus.SENDING;
        this.processingStartedAt = LocalDateTime.now();
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.processingStartedAt = null;
        this.nextRetryAt = null;
    }

    public void markFailed() {
        this.retryCount++;
        this.processingStartedAt = null;
        // 재시도 횟수 초과 시 최종 실패
        if (this.retryCount >= this.maxRetryCount) {
            this.status = NotificationStatus.DEAD;
            this.nextRetryAt = null;
        } else {
            this.status = NotificationStatus.FAILED;
            this.nextRetryAt = LocalDateTime.now().plusSeconds(20L * retryCount);
        }
    }

    public void resetForRetry() {
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.nextRetryAt = null;
        this.processingStartedAt = null;
    }

    public void markRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    // stuck 감지: 처리 시작 후 일정 시간 지난 경우
    public void recoverStuck() {
        this.status = NotificationStatus.FAILED;
        this.processingStartedAt = null;
        this.nextRetryAt = LocalDateTime.now().plusSeconds(20L * retryCount);
    }
}
