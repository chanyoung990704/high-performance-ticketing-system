# 구현 계획: 2주차 9일차 - Redis 심화: TTL 만료 처리 및 대기열 자동 정리

## 1. 목표 (Objective)
Redis Keyspace Notification을 활용하여 토큰 만료 시 다음 대기자를 자동으로 입장시키고, 장기 정체 대기열을 정리하여 메모리 효율성을 극대화합니다. 또한 실시간 모니터링 API를 통해 Redis 상태를 시각화합니다.

## 2. 주요 파일 및 컨텍스트
- **`ticketing-infra`**: `RedisConfig` (Listener 설정), `RedisKeyExpirationListener` (신규), `QueueService` (정리 로직 추가).
- **`ticketing-api`**: `RedisMonitorController` (신규), `PerformanceLoadTest` (검증).
- **`docker-compose.yml`**: Redis 설정 추가.

## 3. 상세 작업 단계

### Step 1: Redis 인프라 및 만료 리스너 설정
1.  **`docker-compose.yml` 수정**: `redis-server --notify-keyspace-events "Ex"` 명령 추가.
2.  **`RedisConfig.java` 수정**: `RedisMessageListenerContainer` 빈 등록 및 직렬화 표준화.
3.  **`RedisKeyExpirationListener.java` 구현**: `active:{eventId}` 만료 시 `queueService.processQueue()` 호출 연동.

### Step 2: 대기열 자동 정리 및 서비스 고도화
1.  **`QueueService.java` 수정**:
    *   `cleanExpiredQueue(Long eventId)`: 30분 초과 대기자 자동 삭제 스케줄러 구현.
    *   `processQueue` 호출 시 활성 토큰에 대한 적절한 TTL 부여 확인.

### Step 3: Redis 모니터링 API 및 검증
1.  **`RedisMonitorController.java` 구현**: 메모리 정보 및 대기열 사이즈 조회 API 작성.
2.  **검증 테스트**:
    *   토큰 강제 만료 후 다음 대기자 입장 여부 확인.
    *   스케줄러 동작 확인.

## 4. 완료 체크리스트
- [ ] Redis 만료 이벤트 수신 및 자동 재처리 확인.
- [ ] 30분 경과 대기열 멤버 자동 삭제 확인.
- [ ] `/api/v1/admin/redis/info`를 통한 실시간 메모리 모니터링 성공.
