# 구현 계획: 2주차 8일차 - Kafka 파티셔닝 전략 고도화

## 1. 목표 (Objective)
기존의 단일 파티션 구조를 파티션 9개 및 컨슈머 그룹 최적화 구조로 개편하여, 이벤트 처리량(Throughput)을 극대화하고 처리 순서를 보장하며 장애 내성(DLQ)을 확보합니다.

## 2. 주요 파일 및 컨텍스트
- **`ticketing-infra`**: `KafkaTopicConfig` (신규), `OutboxRelay` (수정: 파티션 키 적용).
- **`ticketing-api`**: `BookingCreatedConsumer` (수정: 동시성 및 DLQ 적용).
- **`scripts/`**: `check-lag.sh` (신규: 모니터링 스크립트).

## 3. 상세 작업 단계

### Step 1: Kafka 토픽 재설계 및 설정
1.  **`KafkaTopicConfig.java` 생성**:
    - `booking-created`: partitions(9), replicas(1).
    - `booking-confirmed`: partitions(3).
    - `booking-created.DLQ`: partitions(1).
2.  **`application.yml` 수정**:
    - `enable-auto-commit: false`, `ack-mode: MANUAL_IMMEDIATE` 설정.
    - `max-poll-records`, `fetch` 관련 최적화 옵션 추가.

### Step 2: 파티션 키 전략 및 Consumer 고도화
1.  **Producer (OutboxRelay) 수정**:
    - 메시지 전송 시 `eventId`를 파티션 키로 지정하여 동일 이벤트 내 순서 보장.
2.  **Consumer (BookingCreatedConsumer) 수정**:
    - `@KafkaListener`에 `concurrency = "3"` 설정.
    - 수동 커밋(`Acknowledgment.acknowledge()`) 적용.
    - 예외 발생 시 DLQ로 메시지 위임 로직 추가.

### Step 3: DLQ(Dead Letter Queue) Consumer 구현
1.  **`BookingDlqConsumer` 생성**:
    - DLQ 토픽 메시지 소비.
    - 실패한 예약의 상태를 `FAILED`로 업데이트.
    - (선택) 알림 발송 로직 Mocking.

### Step 4: 모니터링 및 검증
1.  **Consumer Lag 확인 스크립트 작성**: `scripts/check-lag.sh`.
2.  **통합 테스트 실행**:
    - 1,000명 이상의 대량 부하를 주어 파티션별 병렬 처리 로그 확인.
    - 의도적 예외 발생을 통한 DLQ 전송 및 상태 변경 확인.

## 4. 완료 체크리스트
- [ ] 토픽 파티션 9개 생성 확인.
- [ ] 3개 이상의 스레드에서 병렬 처리됨을 로그로 확인.
- [ ] 처리 실패 시 DLQ 전송 및 최종 상태 `FAILED` 확인.
- [ ] Lag 모니터링 스크립트 정상 동작 확인.
