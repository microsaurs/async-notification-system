package com.notification.service;

import com.notification.entity.Notification;
import com.notification.entity.NotificationAttempt;
import com.notification.exception.NotificationNotFoundException;
import com.notification.repository.NotificationAttemptRepository;
import com.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSenderService {

    private final NotificationRepository notifRepo;
    private final NotificationAttemptRepository attemptRepo;

    @Async("notificationExecutor")
    @Transactional
    public void sendAsync(Long notifId) {
        Notification notif = notifRepo.findById(notifId)
                .orElseThrow(() -> new NotificationNotFoundException(notifId));
        LocalDateTime startedAt = LocalDateTime.now();
        int attemptNo = notif.getRetryCount() + 1;

        try {
            dispatch(notif);
            notif.markSent();
            attemptRepo.save(NotificationAttempt.success(notif, attemptNo, startedAt));
            log.info("알림 발송 성공 id={} channel={} attemptNo={}", notif.getId(), notif.getChannel(), attemptNo);
        } catch (Exception e) {
            notif.markFailed();
            attemptRepo.save(NotificationAttempt.failure(notif, attemptNo, startedAt, e.getMessage()));
            log.warn("알림 발송 실패 id={} channel={} attemptNo={} reason={}", notif.getId(), notif.getChannel(), attemptNo, e.getMessage());
        }
    }

    private void dispatch(Notification notif) {
        switch (notif.getChannel()) {
            case EMAIL -> sendEmail(notif);
            case IN_APP -> sendInApp(notif);
        }
    }

    private void sendEmail(Notification notif) {
        log.info("[EMAIL] recipientId={} type={}", notif.getRecipientId(), notif.getNotificationType());
        // TODO: 재시도 흐름 테스트용 임시 예외
        // throw new RuntimeException("EMAIL 발송 실패 (테스트)");
    }

    private void sendInApp(Notification notif) {
        log.info("[IN_APP] recipientId={} type={}", notif.getRecipientId(), notif.getNotificationType());
    }
}
