# Day 5 (금) — Kafka Consumer 구현 및 비동기 확정 처리 계획

## 1. Objective
예약 생성 시 발행된 Kafka 이벤트를 소비하여, 실제 데이터베이스의 재고를 원자적으로 차감하고 예약 상태를 `CONFIRMED`로 최종 확정하는 파이프라인을 완성합니다.

## 2. Key Files & Context
- **`ticketing-domain`**:
    - `com.ticketing.domain.event.repository.SeatGradeRepository`: DB 재고 차감 쿼리 추가.
    - `com.ticketing.domain.booking.repository.BookingRepository`: 예약 상태 업데이트 쿼리 추가.
- **`ticketing-api`**:
    - `com.ticketing.api.consumer.BookingCreatedConsumer`: Kafka 메시지 리스너 구현.
    - `src/main/resources/application.yml`: Kafka Consumer 관련 설정.

## 3. Implementation Steps

### Step 1: Repository 기능 확장 (ticketing-domain)
- `SeatGradeRepository`: `@Modifying` 쿼리를 사용하여 `remainCount > 0`인 경우에만 `remainCount`를 1 감소시키는 `decreaseRemainCount` 메서드 추가.
- `BookingRepository`: 예약의 상태(`BookingStatus`)를 업데이트하는 `updateStatus` 메서드 추가.

### Step 2: Kafka Consumer 구현 (ticketing-api)
- `BookingCreatedConsumer` 클래스 생성.
- `@KafkaListener`를 통해 `booking-created` 토픽 수신.
- **로직**:
    1. DB 재고 차감 시도.
    2. 성공 시: 예약 상태 `CONFIRMED`로 변경.
    3. 실패 시(재고 부족 등): 예약 상태 `CANCELLED`로 변경 및 로그 기록.
- 트랜잭션 보장을 위해 `@Transactional` 적용.

### Step 3: 에러 핸들링 및 DLQ 설정
- 처리 실패 시 재시도 전략 검토.
- `Dead Letter Queue (DLQ)` 설정을 위한 `CommonErrorHandler` 또는 설정 추가.

## 4. Verification & Testing
- **상태 전이 확인**: `PENDING` -> `CONFIRMED` 또는 `CANCELLED` 흐름 검증.
- **재고 정확성 확인**: Redis 재고와 DB 재고가 최종적으로 일치하는지 확인.
- **통합 테스트**: 
    1. `POST /api/v1/bookings` 호출.
    2. Consumer 로그 확인.
    3. DB 조회하여 상태 및 재고 감소 확인.

---
작성이 완료되면 이 계획에 따라 구현을 진행하겠습니다.
