# Git 히스토리 재구성 및 사후 커밋 계획 (Day 5 ~ 최종 Fix)

## 1. 개요
작업이 완료된 결과물을 논리적 단위별로 분리하여, 브랜치 생성 및 병합 전략(Branch-per-Task)에 맞게 Git 히스토리를 정리합니다.

## 2. 작업 단위 분할

### Phase 1: feature/kafka-consumer
- **대상**: 
    - `SeatGradeRepository.java`, `BookingRepository.java` (쿼리 메서드 추가)
    - `BookingCreatedConsumer.java` (신규 생성)
- **목표**: 비동기 예약 확정 로직 커밋.

### Phase 2: fix/kafka-infra-config
- **대상**:
    - `docker-compose.yml` (Kafka 리스너 설정 수정)
    - `application.yml` (Kafka Producer/Consumer Json 설정)
- **목표**: 인프라 연결 및 직렬화 설정 오류 수정.

### Phase 3: fix/api-refactoring-and-scheduler
- **대상**:
    - `BookingResponse.java` (DTO 개선)
    - `BookingController.java` (DTO 반환 로직)
    - `QueueService.java` (자동 승인 스케줄러 추가)
- **목표**: OOM 방지 및 테스트 편의성 개선 사항 정리.

## 3. 실행 프로세스
1. 현재 변경 사항 확인 (`git status`).
2. 각 Phase별 브랜치 생성.
3. 관련 파일만 `git add` 및 `git commit`.
4. `main` 브랜치로 병합 및 브랜치 삭제.
5. `git log`를 통한 최종 히스토리 검증.
