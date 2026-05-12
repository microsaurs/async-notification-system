package com.notification.entity;

import com.notification.enums.AttemptStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "notification_attempt",
    indexes = {
        @Index(name = "idx_attempt_notification_id", columnList = "notification_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    // 몇 번째 시도인지
    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AttemptStatus status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static NotificationAttempt success(Notification notification, int attemptNo, LocalDateTime startedAt) {
        NotificationAttempt attempt = new NotificationAttempt();
        attempt.notification = notification;
        attempt.attemptNo = attemptNo;
        attempt.status = AttemptStatus.SUCCESS;
        attempt.startedAt = startedAt;
        attempt.endedAt = LocalDateTime.now();
        return attempt;
    }

    public static NotificationAttempt failure(Notification notification, int attemptNo,
                                              LocalDateTime startedAt, String reason) {
        NotificationAttempt attempt = new NotificationAttempt();
        attempt.notification = notification;
        attempt.attemptNo = attemptNo;
        attempt.status = AttemptStatus.FAILURE;
        attempt.startedAt = startedAt;
        attempt.endedAt = LocalDateTime.now();
        attempt.failureReason = reason;
        return attempt;
    }
}
