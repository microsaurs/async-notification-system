# async-notification-system

비동기 알림 발송 시스템 과제 구현입니다.


## 프로젝트 개요

강의 수강 신청 완료, 결제 확정, 강의 시작 D-1 등의 이벤트 발생 시 사용자에게 EMAIL / IN_APP 채널로 알림을 발송하는 비동기 알림 시스템입니다.

- 알림 등록 요청을 DB에 저장하고, 스케줄러가 주기적으로 픽업하여 비동기로 발송합니다.
- 발송 실패 시 자동 재시도하며, 최대 재시도 횟수 초과 시 DEAD 상태로 보관합니다.
- 멱등성 키를 통해 동일 이벤트에 대한 중복 발송을 방지합니다.


## 기술 스택

| 항목 | 내용 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| ORM | Spring Data JPA |
| Database | MySQL 8.0 (Docker) |
| 비동기 처리 | `@Async` + `ThreadPoolTaskExecutor` |
| 스케줄링 | `@Scheduled` |
| 기타 | Lombok, Docker Compose |

## 실행 방법

**1. MySQL 실행**
```bash
docker-compose up -d
```

**2. 로컬 환경 설정**

실무 보안 관행에 따라 DB 접속 정보는 `src/main/resources/application-local.yml`로 분리하여 관리하며,`.gitignore`에 포함되어 있습니다.  
과제 테스트를 위해 아래 내용으로 직접 생성해주세요.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/notification?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: root
    password: password
```

**3. 애플리케이션 실행**
```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

**4. API 테스트**

`http/notification.http` 파일에 전체 테스트 케이스가 작성되어 있으며, IntelliJ HTTP Client로 실행할 수 있습니다.  
IntelliJ가 없는 경우 curl, Postman 등 HTTP 클라이언트로도 동일하게 테스트할 수 있습니다.

## 요구사항 해석 및 가정

**eventId 필수값 처리**
- `eventId`는 중복 발송 방지를 위한 **멱등성 키 생성에 반드시 필요**하므로 필수값으로 처리했습니다.
- upstream 서비스(수강 신청, 결제 등)가 이벤트 발생 시 고유한 `eventId`를 생성해 전달한다고 가정합니다.

**EMAIL 읽음 처리**
- EMAIL은 발송 후 사용자의 수신함에서 열람되므로 서버에서 직접 읽음 여부를 추적하기 어렵습니다.
- 실제 환경에서는 SendGrid webhook을 통해 이메일 오픈 이벤트를 수신하는 방식으로 구현 가능합니다.
- 이번 과제에서는 `PATCH /api/notifications/{id}/read` API를 IN_APP 채널에만 구현하였습니다.

**발송 시뮬레이션**
- 실제 외부 API(SendGrid, FCM 등) 연동 없이 `log.info()`로 발송 내용을 출력하는 방식으로 대체했습니다.

## 설계 결정과 이유

### 스케줄러 패턴

스케줄러 구현 방식으로 두 가지를 검토했습니다.

- **A안**  
하나의 트랜잭션 안에서 픽업 + 발송을 모두 처리  
트랜잭션이 길어지고, 비동기 발송을 트랜잭션 커밋 전에 시작하면 다중 인스턴스 환경에서 중복 픽업이 발생할 수 있습니다.
- **B안**  
픽업(SENDING 상태 전환)을 트랜잭션으로 커밋한 뒤, 발송은 별도 `@Async` 스레드에서 처리

B안을 채택했습니다.

```
@Scheduled → pickUpForSending() [트랜잭션 커밋] → sendAsync() [@Async 비동기]
```

**이유:**
- 픽업 트랜잭션이 커밋된 후에 비동기 발송을 시작하므로, 다중 인스턴스 환경에서 동일 건이 중복 픽업되지 않습니다.
- 발송 중 서버가 죽어도 SENDING 상태가 DB에 남아 stuck 복구 로직이 처리할 수 있습니다.

### Self-invocation 방지를 위한 서비스 분리

`@Transactional`은 Spring 프록시 기반으로 동작하므로 같은 클래스 내부에서 호출하면 트랜잭션이 적용되지 않습니다.  
이를 방지하기 위해 `NotificationPickUpService`를 별도 빈으로 분리했습니다.

### Detached Entity 방지를 위한 ID 전달

픽업 트랜잭션이 종료된 후 반환된 엔티티는 detached 상태가 됩니다.  
`sendAsync()`에 엔티티 대신 ID를 전달하고, 새 트랜잭션 내에서 다시 조회하여 변경사항이 DB에 반영되도록 했습니다.

### 다중 인스턴스 환경의 중복 처리 방지

픽업 대상 조회 시 `@Lock(PESSIMISTIC_WRITE)`를 적용하여 트랜잭션 내에서 후보 알림 row를 잠급니다.  
잠금을 확보한 인스턴스만 해당 알림을 `SENDING` 상태로 변경하고 커밋한 뒤 비동기 발송을 시작하므로,  
다중 인스턴스 환경에서도 동일 알림이 동시에 선점되는 것을 방지할 수 있습니다.  

### 재시도 정책

| 항목 | 값 |
|---|---|
| 최대 재시도 횟수 | 3회 |
| 재시도 간격 | 20초 × 재시도 횟수 (1회: 20초, 2회: 40초) |
| 스케줄러 주기 | 30초 |
| stuck 기준 | SENDING 상태 5분 이상 지속 |

재시도 간격을 스케줄러 주기(30초)보다 짧게 설정(20초 × n)하여 다음 스케줄러 사이클에서 픽업되도록 했습니다.  
`@Scheduled`는 단순 주기 픽업에 충분하나, 대규모 발송으로 확장 시 Spring Batch나 Quartz 클러스터 스케줄링으로 전환을 고려할 수 있습니다.

### 서버 재시작 후 재처리

알림 상태를 DB에 저장하고 있기 때문에, 서버가 재시작되더라도 PENDING / FAILED 상태의 알림은 다시 조회해서 처리할 수 있도록 했습니다.  
재시작 전 SENDING 상태였던 건은 stuck 복구 스케줄러(1분 주기)가 FAILED로 되돌려 재처리합니다.

### DEAD 상태 및 수동 재시도 정책

자동 재시도 횟수를 모두 소진한 알림은 DEAD 상태로 격리합니다.  
계속 실패하는 알림이 무한 재시도로 남아 있으면, 스레드와 쿼리를 계속 점유하게 되어 운영 부담이 커질 수 있다고 판단했습니다.  
운영자는 원인 파악 후 `POST /api/notifications/{id}/retry`로 수동 재시도할 수 있으며,  
이 때 `retryCount`는 0으로 초기화되어 자동 재시도 기회를 다시 부여합니다.

## 개선 의견

**알림 취소 API**  
현재 등록된 알림을 취소하는 API가 없습니다.
특히 예약 발송의 경우, 잘못 등록된 알림을 발송 전에 취소할 수 있어야 운영 측면에서 안전하다고 생각했습니다.  
실제 구현 시에는 아래와 같은 정책이 먼저 정의되어야 한다고 판단했습니다.  

- 어떤 상태까지 취소를 허용할 것인지 (`PENDING`만 허용할지, `SENDING`도 허용할지)  
- 취소 요청 권한은 어떻게 검증할 것인지  
- 이미 발송 중인 알림은 어떻게 처리할 것인지  

이번 과제에서는 핵심 요구사항인 비동기 처리, 재시도, 중복 방지에 집중하기 위해 제외했습니다.  

**DEAD 알림 운영 처리**  
현재는 DEAD 상태가 된 알림을 운영자가 직접 조회해야만 확인할 수 있습니다.  
실제 운영 환경에서는 DEAD 상태가 일정 개수 이상 발생하면 Slack 또는 이메일로 알람을 보내고,  
관리자 화면에서 실패 원인 확인이나 일괄 재시도 기능 등을 함께 제공하는 방향이 더 적절하다고 생각했습니다.  

## 미구현 / 제약사항

| 항목 | 내용                                                                                                                   |
|---|----------------------------------------------------------------------------------------------------------------------|
| 실제 발송 연동 | `log.info()`로 대체. SendGrid, FCM 등 외부 API 연동 시 `dispatch()` 내부만 교체하면 됩니다.                                             |
| EMAIL 읽음 처리 | SendGrid open webhook으로 구현 가능하나 이번 과제에서는 미구현                                                                         |
| 동시 읽음 처리 | 여러 기기에서 동시에 읽음 요청 시 최종 결과가 동일(`isRead=true`)하므로 현재는 허용. <br/> 트래픽 증가 시 `PESSIMISTIC_WRITE` 락 또는 낙관적 락(`@Version`)으로 개선 가능 |

---

## AI 활용 범위

Claude Code를 개발 보조 도구로 활용했습니다.

비동기 처리 구조, 트랜잭션 경계, self-invocation 같은 **설계 이슈를 검토**할 때 주로 활용했고,
DTO / Repository 등 반복적인 **보일러플레이트 코드 작성**에도 일부 사용했습니다.  

다만 실제 구현 과정에서는 직접 동작을 확인하면서 구조를 수정했고,
아래와 같은 핵심 정책과 설계는 직접 판단하여 구현했습니다.

- 스케줄러 처리 구조 선택
- 재시도 정책 및 retry interval 결정
- 멱등성 키 정책 (`eventId` 필수 처리)
- stuck recovery 방식
- DEAD 상태 및 수동 재시도 정책
- 읽음 처리 정책 및 트레이드오프 판단

## API 목록 및 예시

자세한 API 명세는 [API.md](./API.md)를 참고하세요.

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/notifications` | 알림 발송 요청 등록 |
| GET | `/api/notifications` | 사용자 알림 목록 조회 |
| GET | `/api/notifications/{id}` | 알림 상태 조회 |
| POST | `/api/notifications/{id}/retry` | DEAD 알림 수동 재시도 |
| PATCH | `/api/notifications/{id}/read` | 알림 읽음 처리 (IN_APP) |

## 데이터 모델 설명

자세한 ERD는 [ERD.md](./ERD.md)를 참고하세요.

**notification**
- 알림 발송 요청 1건을 저장합니다.
- `idempotency_key` UNIQUE 제약으로 중복 발송을 방지합니다.
- `processing_started_at`으로 stuck 감지에 활용합니다.

**notification_attempt**
- 발송 시도 이력을 저장합니다.
- 1건의 알림에 여러 번의 시도 이력이 쌓입니다.
- 성공/실패 여부와 실패 사유를 기록합니다.

## 테스트 실행 방법

`http/notification.http` 파일을 IntelliJ HTTP Client로 실행합니다.

| 테스트 케이스 | 설명 |
|---|---|
| 알림 발송 요청 등록 | 정상 등록 및 200 응답 확인 |
| 중복 요청 | 동일 eventId 재요청 시 기존 건 반환 확인 |
| eventId 누락 | 필수값 누락 시 400 에러 확인 |
| 알림 상태 조회 | status, sentAt, isRead, readAt 필드 확인 |
| 존재하지 않는 id 조회 | 404 에러 확인 |
| 알림 목록 조회 (전체) | recipientId 기준 전체 목록 확인 |
| 알림 목록 조회 (안읽음 필터) | isRead=false 필터 확인 |
| 알림 목록 조회 (읽음 필터) | isRead=true 필터 확인 |
| 예약 발송 | scheduledAt 설정 후 해당 시각에 발송되는지 확인 |
| 수동 재시도 | DEAD 상태 알림 재시도 후 PENDING → SENT 확인 |
| 수동 재시도 - DEAD 아닌 상태 | 400 에러 확인 |
| 읽음 처리 | IN_APP + SENT 상태 알림 읽음 처리 후 isRead: true 확인 |
| 읽음 처리 - EMAIL 채널 | 400 에러 확인 |
| 읽음 처리 - 존재하지 않는 id | 404 에러 확인 |
