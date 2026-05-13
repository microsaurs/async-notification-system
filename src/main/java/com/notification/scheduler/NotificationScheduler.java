package com.notification.scheduler;

import com.notification.service.NotificationPickUpService;
import com.notification.service.NotificationSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationPickUpService pickUpService;
    private final NotificationSenderService senderService;

    @Scheduled(fixedDelayString = "${notification.retry.scheduler-interval-ms}")
    public void process() {
        List<Long> ids = pickUpService.pickUpForSending();
        if (ids.isEmpty()) return;

        log.info("스케줄러 픽업 건수={}", ids.size());
        ids.forEach(senderService::sendAsync);
    }
}
