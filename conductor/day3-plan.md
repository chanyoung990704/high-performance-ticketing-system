# 구현 계획: 2주차 3일차 - Redis 대기열 핵심 로직 구현

## 1. 목표
Redis의 Sorted Set, Set, String 자료구조를 사용하여 대기열 관리 및 원자적 재고 차감 로직을 구현합니다.

## 2. 상세 작업 단계

### Step 1: 환경 설정 및 DTO
- `RedisConfig` 설정 확인 및 보완.
- `QueueStatusResponse` DTO 작성 (ticketing-api).

### Step 2: QueueService 구현 (ticketing-infra)
- `enterQueue`: Sorted Set 기반 대기열 추가.
- `processQueue`: `@Scheduled` 기반 활성 토큰 발급.
- `getStatus`: 현재 순위 및 입장 가능 여부 조회.

### Step 3: 재고 관리 (Lua Script)
- `decreaseStock`: Lua Script를 이용한 원자적 재고 감소 로직 구현.

### Step 4: 검증 및 테스트
- 대기열 순서 보장 테스트.
- 100명 이상 동시 요청 시 재고 감소 정확성 테스트 (Race Condition 확인).
- Redis Insight를 통한 실시간 모니터링 확인.

## 3. 완료 체크리스트
- [ ] Redis 자료구조 설계 준수 여부.
- [ ] Lua Script 원자성 보장 확인.
- [ ] 스케줄러를 통한 토큰 전환 로직 정상 동작 확인.
