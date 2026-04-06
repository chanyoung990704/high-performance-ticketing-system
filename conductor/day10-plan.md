# 구현 계획: 2주차 10일차 - TPS 10,000 달성을 위한 단계별 성능 튜닝

## 1. 목표 (Objective)
기본 인프라 설정을 정밀 튜닝하고 Java 21 Virtual Thread를 도입하여, 시뮬레이션 환경(H2/Embedded Kafka) 및 로컬 환경에서 목표 TPS 10,000건을 달성하고 각 최적화 단계의 기여도를 측정합니다.

## 2. 상세 작업 단계 및 측정 시나리오

### Step 1: 인프라 및 애플리케이션 핵심 튜닝 (application.yml)
1.  **HikariCP 최적화**: 커넥션 풀을 10에서 30으로 확장하고 누수 감지 및 타임아웃 설정 강화.
2.  **Kafka Producer 배치 최적화**: `batch-size(64KB)`, `linger-ms(5ms)`, `snappy` 압축 적용으로 네트워크 처리량 증대.
3.  **Tomcat & Virtual Thread 활성화**: 
    *   `spring.threads.virtual.enabled: true` 설정.
    *   Tomcat 최대 스레드 및 수용 가능한 대기 큐(`accept-count`) 조정.

### Step 2: 성능 측정 테스트 코드 고도화 (`PerformanceLoadTest`)
1.  **부하 강도 조절**: 가상 사용자(VU)를 1,000명에서 10,000명으로 증폭.
2.  **단계별 측정 로직**: 각 최적화 단계 전/후의 TPS를 비교할 수 있도록 테스트 코드 및 로그 보완.

### Step 3: 5단계 TPS 측정 및 기록 (Benchmark)
- **단계 1**: 현재 상태 (베이스라인) 측정.
- **단계 2**: HikariCP 조정 후 증분 측정.
- **단계 3**: Kafka Producer 배치 설정 후 증분 측정.
- **단계 4**: Virtual Thread 활성화 후 기하급수적 성능 향상 확인.
- **단계 5**: 파티셔닝(9개) + 컨슈머 동시성(3개) 최종 시너지 확인.

### Step 4: 결과 문서화 (README.md)
1.  사용자 제공 템플릿에 측정된 수치 기록.
2.  각 튜닝 포인트별 성능 향상 원인(Interview-Ready) 정리.

## 3. 완료 체크리스트
- [ ] 10,000 TPS 도달 여부 확인 (또는 하드웨어 한계치까지 도달).
- [ ] Virtual Thread 도입 전/후 유의미한 성능 차이(최소 50% 이상) 확인.
- [ ] 10,000건 부하에서도 DB 정합성 및 Kafka 메시지 유실 없음 확인.
- [ ] 모든 측정 지표의 README 반영 완료.
