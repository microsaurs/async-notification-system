# ERD

```mermaid
erDiagram
    NOTIFICATION ||--o{ NOTIFICATION_ATTEMPT : has

    NOTIFICATION {
        BIGINT id PK "NOT NULL"
        BIGINT recipient_id "NOT NULL"
        VARCHAR notification_type "NOT NULL"
        VARCHAR channel "NOT NULL"
        VARCHAR event_id "NOT NULL"
        VARCHAR reference_id "nullable"
        VARCHAR status "NOT NULL"
        VARCHAR idempotency_key UK "NOT NULL"
        INT retry_count "NOT NULL"
        INT max_retry_count "NOT NULL"
        DATETIME next_retry_at "nullable"
        DATETIME processing_started_at "nullable"
        BOOLEAN is_read "NOT NULL"
        DATETIME read_at "nullable"
        DATETIME scheduled_at "nullable"
        DATETIME sent_at "nullable"
        DATETIME created_at "NOT NULL"
        DATETIME updated_at "NOT NULL"
    }

    NOTIFICATION_ATTEMPT {
        BIGINT id PK "NOT NULL"
        BIGINT notification_id FK "NOT NULL"
        INT attempt_no "NOT NULL"
        VARCHAR status "NOT NULL"
        TEXT failure_reason "nullable"
        DATETIME started_at "NOT NULL"
        DATETIME ended_at "nullable"
        DATETIME created_at "NOT NULL"
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
