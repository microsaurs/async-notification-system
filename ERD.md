# ERD

```mermaid
erDiagram
    NOTIFICATION ||--o{ NOTIFICATION_ATTEMPT : has

    NOTIFICATION {
        BIGINT id PK
        BIGINT recipient_id
        VARCHAR notification_type
        VARCHAR channel
        VARCHAR event_id
        VARCHAR reference_id
        VARCHAR status
        VARCHAR idempotency_key UK
        INT retry_count
        INT max_retry_count
        DATETIME next_retry_at
        DATETIME processing_started_at
        BOOLEAN is_read
        DATETIME read_at
        DATETIME scheduled_at
        DATETIME sent_at
        DATETIME created_at
        DATETIME updated_at
    }

    NOTIFICATION_ATTEMPT {
        BIGINT id PK
        BIGINT notification_id FK
        INT attempt_no
        VARCHAR status
        TEXT failure_reason
        DATETIME started_at
        DATETIME ended_at
        DATETIME created_at
    }
```

## 상태 전이

```
PENDING → SENDING → SENT
PENDING → SENDING → FAILED → SENDING → SENT
PENDING → SENDING → FAILED → ... → DEAD
```

| 상태 | 설명 |
|---|---|
| PENDING | 발송 대기 |
| SENDING | 처리 중 |
| SENT | 발송 완료 |
| FAILED | 실패 (재시도 대기) |
| DEAD | 최종 실패 (재시도 횟수 초과) |

## 테이블 관계

- `NOTIFICATION` 1개에 `NOTIFICATION_ATTEMPT` 여러 개 (1:N)
- 발송 시도마다 `NOTIFICATION_ATTEMPT`에 이력이 쌓임
- `idempotency_key` UNIQUE 제약으로 중복 발송 방지
