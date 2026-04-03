# 구현 계획: Transactional Outbox 패턴 적용

## 1. 목표 (Objective)
예약 생성(DB)과 이벤트 발행(Kafka)을 하나의 트랜잭션으로 묶어, 시스템 장애 시에도 메시지 유실이나 데이터 불일치가 발생하지 않도록 원자성을 보장합니다.

## 2. 주요 파일 및 컨텍스트
- **`ticketing-domain`**: `OutboxEvent` 엔티티 추가.
- **`ticketing-infra`**: `V7__create_outbox_event.sql` 마이그레이션 및 `MessageRelay` 스케줄러 구현.
- **`ticketing-api`**: `BookingService` 로직 수정 (Direct Send -> Outbox Save).

## 3. 상세 작업 단계

### Step 1: Outbox 테이블 및 엔티티 생성
1.  **Flyway 마이그레이션 (`V7`)**:
    - `id`, `aggregate_type`, `aggregate_id`, `payload` (JSON), `status` (INIT, SENT, FAIL), `created_at` 컬럼 정의.
2.  **`OutboxEvent` 엔티티**: 도메인 모듈에 작성.

### Step 2: 서비스 로직 수정
1.  **`BookingService.createBooking`**:
    - `kafkaTemplate.send()` 코드를 제거.
    - `outboxRepository.save()`를 통해 이벤트를 DB에 저장하도록 변경 (기존 `@Transactional` 내에서 수행).

### Step 3: Message Relay 구현 (ticketing-infra)
1.  **`OutboxRelay` 클래스**:
    - `@Scheduled` (예: 500ms 단위)로 `status = 'INIT'`인 이벤트를 조회.
    - `kafkaTemplate.send()`를 사용하여 실제 Kafka로 전송.
    - 전송 성공 시 `status = 'SENT'`로 업데이트.
    - (선택 사항) 실패 시 재시도 로직 또는 `FAIL` 처리.

### Step 4: 정합성 검증 테스트
1.  **테스트 시나리오**:
    - 예약 성공 후 Kafka 일시 중단 시뮬레이션 -> DB에 Outbox가 남아있는지 확인.
    - Kafka 복구 후 스케줄러에 의해 메시지가 정상 발행되는지 확인.

## 4. 완료 체크리스트
- [ ] `Booking` 저장과 `OutboxEvent` 저장이 동일 트랜잭션에서 수행됨을 확인.
- [ ] 스케줄러를 통한 메시지 발행 및 `SENT` 상태 변경 확인.
- [ ] Kafka Consumer가 아웃박스를 거친 이벤트를 정상 소비하여 예약 확정함을 확인.
