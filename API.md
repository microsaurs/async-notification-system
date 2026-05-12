# API 명세

## 공통
- Base URL: `/api`
- Content-Type: `application/json`
- 인증: `X-User-Id` 헤더로 사용자 식별

---

## 알림 발송 요청 등록

**POST** `/api/notifications`

```json
// Request
{
  "recipientId": 1,
  "notificationType": "ENROLLMENT_COMPLETE",
  "channel": "EMAIL",
  "eventId": "event-123",
  "referenceId": "lecture-456",
  "scheduledAt": null
}

// Response 200
{
  "id": 1,
  "status": "PENDING",
  "createdAt": "2026-05-12T10:00:00"
}
```

> 동일한 이벤트로 중복 요청이 들어올 경우 기존 건을 그대로 반환한다.  
> `scheduledAt`이 null이면 즉시 처리, 값이 있으면 해당 시각에 발송된다.

---

## 알림 상태 조회

**GET** `/api/notifications/{id}`

```json
// Response 200
{
  "id": 1,
  "recipientId": 1,
  "notificationType": "ENROLLMENT_COMPLETE",
  "channel": "EMAIL",
  "status": "SENT",
  "retryCount": 0,
  "nextRetryAt": null,
  "sentAt": "2026-05-12T10:00:05",
  "createdAt": "2026-05-12T10:00:00"
}
```

---

## 사용자 알림 목록 조회

**GET** `/api/notifications?recipientId={recipientId}&isRead={true|false}`

| 파라미터 | 필수 | 설명 |
|---|---|---|
| recipientId | Y | 수신자 ID |
| isRead | N | 읽음 여부 필터 (생략 시 전체 조회) |

```json
// Response 200
[
  {
    "id": 1,
    "notificationType": "ENROLLMENT_COMPLETE",
    "channel": "IN_APP",
    "status": "SENT",
    "isRead": false,
    "createdAt": "2026-05-12T10:00:00"
  }
]
```

---

## 읽음 처리

**PATCH** `/api/notifications/{id}/read`

> IN_APP 채널 알림만 가능

```json
// Response 200
{
  "id": 1,
  "isRead": true,
  "readAt": "2026-05-12T10:05:00"
}
```
