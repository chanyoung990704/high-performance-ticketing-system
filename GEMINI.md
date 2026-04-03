# 📋 프로젝트 전략 및 컨벤션 (Mandatory Rules)

## 📁 브랜치 전략 (Branch Strategy)
- `main`: 배포 가능한 안정 버전.
- `develop`: 개발 통합 브랜치.
- `feature/*`: 기능 단위 개발 브랜치.
  - `feature/week1-setup`
  - `feature/week1-redis-queue`
  - `feature/week1-booking-api`
  - `feature/week1-kafka-consumer`
  - `feature/week1-performance-test`

## 💬 커밋 컨벤션 (Commit Convention)
- `feat`: 새로운 기능 구현. (예: `feat: Redis 대기열 진입 API 구현`)
- `fix`: 버그 수정. (예: `fix: 재고 감소 Lua Script 버그 수정`)
- `perf`: 성능 개선 및 테스트. (예: `perf: JMeter 1,000 TPS 달성`)
- `docs`: 문서 수정. (예: `docs: ERD 및 README 업데이트`)
- `refactor`: 코드 리팩토링.
- `test`: 테스트 코드 추가 및 수정.

## 🏗️ 주요 기술적 의사결정 (Technical Decisions)
| 결정 사항 | 선택 이유 |
| :--- | :--- |
| **재고 관리 위치 (Redis Lua Script)** | DB UPDATE 락 경합으로 인한 TPS 급락 방지 및 원자성 보장 |
| **Kafka 비동기 처리** | 예매 이벤트 비동기 처리를 통해 API 응답 속도 개선 및 DB 부하 분산 |
| **PENDING 상태 존재** | Saga 패턴 준비 및 Kafka 처리 전 중간 상태 표현 |
| **KRaft 모드 (Kafka)** | Zookeeper 제거를 통한 로컬 환경 단순화 및 Kafka 3.x 표준 준수 |
| **open-in-view: false** | OSIV 비활성화를 통한 커넥션 풀 낭비 방지 및 실무 표준 준수 |

## 📊 성능 테스트 시나리오 (JMeter)
### 시나리오 1: 대기열 진입 부하 테스트
- **가상 사용자**: 1,000명
- **Ramp-Up**: 10초
- **Endpoint**: `POST /api/v1/queues/{eventId}/enter`

### 시나리오 2: 동시 예매 부하 테스트
- **조건**: 재고 500개 설정, 1,000명 동시 예매 시도.
- **기대 결과**: 정확히 500건 CONFIRMED, 500건 SOLD_OUT. DB 정합성 100% 일치.

## 🎯 1주차 핵심 어필 포인트 (Pitching Points)
- Redis Lua Script를 활용한 Race Condition 해결 과정 설명.
- Kafka를 활용한 API 응답 시간과 DB 처리 로직의 Decoupling 시연.
- JMeter를 이용한 실제 동시성 테스트 수치 확보 및 분석.
- 설계 의사결정 과정의 문서화 및 회고 기록.
