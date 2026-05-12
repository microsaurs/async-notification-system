package com.notification.enums;

public enum NotificationStatus {
    PENDING,   // 발송 대기
    SENDING,   // 처리 중
    SENT,      // 발송 완료
    FAILED,    // 실패 (재시도 가능)
    DEAD       // 최종 실패 (재시도 횟수 초과)
}
