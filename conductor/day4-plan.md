# 구현 계획: 2주차 4일차 - API 레이어 구현 및 Kafka 연동

## 1. 목표
대기열 진입부터 예매 시도까지의 전체 프로세스를 처리하는 REST API를 구현하고, 비동기 처리를 위한 Kafka 이벤트를 발행합니다.

## 2. 상세 작업 단계

### Step 1: Repository 및 예외 클래스 작성
- `BookingRepository`, `SeatGradeRepository` 등 인터페이스 생성.
- `NotAdmittedException`, `SoldOutException` 등 커스텀 예외 클래스 작성.

### Step 2: QueueService 기능 확장 (ticketing-infra)
- `isAdmitted`, `isAlreadyBooked`, `markAsBooked` 메서드 추가.

### Step 3: BookingService 구현 (ticketing-api)
- 예매 프로세스 (검증 -> 재고 감소 -> PENDING 저장 -> Kafka 발행) 구현.

### Step 4: Controller 및 Exception Handler 구현
- `QueueController`, `BookingController` 작성.
- `@RestControllerAdvice`를 통한 글로벌 예외 처리.

### Step 5: 문서화 및 최종 확인
- Swagger(SpringDoc) 설정.
- Postman 또는 HTTP 테스트 파일을 이용한 API 동작 검증.

## 3. 완료 체크리스트
- [ ] 4개 API (진입, 상태조회, 예매시도, 결과조회) 정상 동작.
- [ ] 예외 케이스 처리 및 에러 메시지 확인.
- [ ] Kafka 이벤트 발행 확인 (로그 기반).
- [ ] Swagger UI 접속 및 명세 확인.
