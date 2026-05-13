package com.notification.repository;

import com.notification.entity.Notification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(Long recipientId, boolean isRead);

    // 스케줄러 처리 대상 조회 - PENDING/FAILED 조건이 달라서 분리
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Notification n WHERE " +
           "(n.status = 'PENDING' AND (n.scheduledAt IS NULL OR n.scheduledAt <= :now)) OR " +
           "(n.status = 'FAILED' AND (n.nextRetryAt IS NULL OR n.nextRetryAt <= :now))")
    List<Notification> findPickUpTargetsWithLock(@Param("now") LocalDateTime now);

    // SENDING 상태로 일정 시간 이상 멈춰있는 건 복구 대상
    @Query("SELECT n FROM Notification n WHERE n.status = 'SENDING' AND n.processingStartedAt < :threshold")
    List<Notification> findStuckSendingNotifications(@Param("threshold") LocalDateTime threshold);
}
