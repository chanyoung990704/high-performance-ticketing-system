# [재검토] 구현 계획: 2주차 2일차 - 도메인 설계 및 JPA 엔티티 구현

## 1. 목표
Flyway를 통해 DB 스키마 형상을 관리하고, 설계된 ERD를 바탕으로 JPA 엔티티를 완벽하게 매핑합니다.

## 2. 상세 작업 단계

### Step 1: 인프라 설정 (Flyway)
- `ticketing-infra`에 Flyway 의존성 추가.
- `application.yml`에 Flyway 설정 (enabled: true) 반영.

### Step 2: DB 마이그레이션 파일 작성
- `V1__create_event.sql` ~ `V5__create_queue_history.sql` 작성.

### Step 3: JPA 엔티티 구현 (ticketing-domain)
- `BaseTimeEntity` (생성일, 수정일 공통화)
- `Event`, `SeatGrade`, `User`, `Booking`, `QueueHistory` 엔티티 작성.
- Enum 타입 정의: `EventStatus`, `BookingStatus`, `QueueResult`.

### Step 4: 유효성 검증 (핵심)
1. **컴파일 확인**: `./gradlew build` 실행.
2. **Flyway 검증**: 실제 DB에 테이블 생성 확인.
3. **Hibernate Validation**: `ddl-auto: validate`를 통해 엔티티-테이블 매핑 정합성 검증.

## 3. 완료 체크리스트
- [ ] Flyway 마이그레이션 성공.
- [ ] 전체 엔티티 컴파일 및 유효성 검사 통과.
- [ ] 설계 의사결정 문서 작성 (Notion 대용으로 `docs/design-notes.md`).
