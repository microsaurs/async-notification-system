package com.notification.repository;

import com.notification.entity.NotificationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, Long> {

    List<NotificationAttempt> findByNotificationIdOrderByAttemptNoAsc(Long notificationId);

    // 마지막 시도 번호 조회 - 다음 attemptNo 계산용
    @Query("SELECT MAX(a.attemptNo) FROM NotificationAttempt a WHERE a.notification.id = :notifId")
    Optional<Integer> findLastAttemptNo(@Param("notifId") Long notifId);
}
