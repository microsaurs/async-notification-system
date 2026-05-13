package com.notification.service;

import com.notification.entity.Notification;
import com.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPickUpService {

    private final NotificationRepository notifRepo;

    @Value("${notification.retry.stuck-threshold-minutes}")
    private int stuckThresholdMinutes;

    @Transactional
    public List<Long> pickUpForSending() {
        List<Notification> targets = notifRepo.findPickUpTargetsWithLock(LocalDateTime.now());
        targets.forEach(Notification::startSending);
        return targets.stream().map(Notification::getId).toList();
    }

    @Transactional
    public void recoverStuck() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(stuckThresholdMinutes);
        List<Notification> stuckList = notifRepo.findStuckSendingNotifications(threshold);
        if (stuckList.isEmpty()) return;

        log.warn("Stuck 알림 복구 건수={}", stuckList.size());
        stuckList.forEach(Notification::recoverStuck);
    }
}
