# [실전] 구현 계획: 실제 JMeter 성능 측정 및 분석

## 1. 목표 (Objective)
시뮬레이션 수치가 아닌, 실제 로컬 환경의 자원을 사용하여 시스템의 한계 TPS와 동시성 제어의 완벽성을 검증합니다.

## 2. 사전 준비 (Prerequisites)
- **도구 설치**: Docker를 이용한 JMeter 실행 또는 로컬 JMeter 설치.
- **데이터 셋업**:
    - 테스트용 사용자 1,000명 사전 생성 (`V6__create_test_users.sql` 또는 Script 활용).
    - 테스트용 이벤트 및 좌석 등급(재고 500개) 등록.
- **인프라 구동**: `docker compose up -d` (MySQL, Redis, Kafka).

## 3. 상세 작업 단계 (Implementation Steps)

### Step 1: JMeter 테스트 스크립트(.jmx) 작성
1.  **Thread Group 설정**: 가상 사용자(VU) 1,000명, Ramp-up 10초.
2.  **CSV Data Set Config**: 사전 생성된 1,000명의 userId를 읽어오도록 설정.
3.  **HTTP Request Sampler**: 
    - `POST /api/v1/queues/{eventId}/enter` (대기열 진입)
    - `POST /api/v1/bookings` (예매 시도) - 대기열 입장 토큰(Header) 포함 필요.

### Step 2: 테스트 실행 (CLI Mode)
- GUI의 부하를 줄이기 위해 CLI 모드로 실행:
  ```bash
  jmeter -n -t ticketing_test.jmx -l results.jtl -e -o report/
  ```
- (Docker 사용 시): `justb4/jmeter` 등 이미지를 활용하여 컨테이너 내에서 실행.

### Step 3: 결과 분석 및 기록
1.  **Dashboard Report 분석**:
    - Response Time Percentiles (90%, 95%, 99%).
    - Active Threads Over Time 대비 TPS 변동 추이.
2.  **데이터 정합성 최종 검증 (DB Query)**:
    ```sql
    SELECT COUNT(*) FROM booking WHERE status = 'CONFIRMED'; -- 정확히 500건 확인
    SELECT remain_count FROM seat_grade WHERE id = :gradeId; -- 0 확인
    ```

### Step 4: README 최신화
- 시뮬레이션(Simulated) 표시를 제거하고, 실제(Actual) 측정값으로 업데이트.
- 병목 현상(Latency Spike 등) 발생 지점 분석 내용 추가.

## 4. 완료 체크리스트
- [ ] JMeter CLI 모드로 1,000명 동시 접속 테스트 완주.
- [ ] 데이터베이스 조회 결과와 Redis 재고 차감 결과의 100% 일치 확인.
- [ ] 실제 측정된 TPS(Throughput) 기록 완료.
- [ ] 보고서(HTML Report) 생성 및 주요 지표 캡처.
