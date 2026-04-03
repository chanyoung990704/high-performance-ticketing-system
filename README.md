# 🎟️ High-Performance Ticketing System

본 프로젝트는 초당 수만 건 이상의 대규모 동시 접속이 발생하는 티켓팅 환경을 안정적으로 처리하기 위한 **확장성 있는 백엔드 시스템**입니다. Redis 기반의 대기열 시스템과 Kafka를 이용한 비동기 예약 확정 아키텍처를 핵심으로 합니다.

## 🚀 Key Achievements

- **Redis 기반 순서 보장 대기열**: `Sorted Set`을 활용하여 진입 시점 기준의 공정한 대기 순위를 보장하며, DB 부하를 원천 차단합니다.
- **원자적 재고 관리 (Lua Script)**: Redis의 싱글 스레드 특성과 Lua Script를 결합하여 **Race Condition이 없는 100% 정확한 재고 차감**을 구현하였습니다.
- **비동기 예약 파이프라인 (Kafka)**: 사용자 응답 속도를 극대화하기 위해 예약 요청과 실제 DB 반영(확정) 로직을 Kafka를 통해 분리(Decoupling)하였습니다.
- **멀티 모듈 아키텍처**: 레이어 간 관심사를 엄격히 분리하여 유지보수성과 확장성을 높였습니다.

## 🛠️ Tech Stack

- **Backend**: Java 21, Spring Boot 3.3.4
- **Persistence**: MySQL 8.0, Spring Data JPA, Flyway (DB Migration)
- **Infrastructure**: Redis 7.2 (Queue/Stock), Kafka 7.6.0 (Async Worker)
- **Environment**: Docker & Docker Compose
- **Docs**: Swagger (SpringDoc)

## 🏗️ Architecture & Data Flow

1. **Queue Entry**: 사용자가 대기열 진입 요청 시 Redis Sorted Set에 타임스탬프와 함께 등록.
2. **Admission Control**: 스케줄러가 대기열 상위 사용자를 활성 토큰 세트로 이동 (입장 권한 부여).
3. **Atomic Booking**: 
   - 입장 허가된 사용자가 예매 시도 시, Redis Lua Script로 실시간 재고 확인 및 즉시 차감.
   - DB에는 `PENDING` 상태로 예약 레코드 선저장.
4. **Asynchronous Finalization**: 
   - 예약 생성 이벤트를 Kafka로 발행.
   - Kafka Consumer가 이벤트를 소비하여 DB 재고 최종 차감 및 상태를 `CONFIRMED`로 변경.

## 📊 성능 테스트 결과

### 테스트 환경
- **OS**: Ubuntu 22.04 (WSL2)
- **JVM**: Java 21, `-Xmx512m`
- **DB**: MySQL 8.0 (Docker, 로컬) / H2 (Test Mode)
- **Redis**: 7.2 Single Node (Docker, 로컬)
- **테스트 도구**: JUnit5 + MockMvc (1,000 Concurrent Users)

### 대기열 진입 부하 테스트 (1,000 동시 사용자)
| 지표 | 결과 |
|------|------|
| TPS | **320.0 req/s** |
| 평균 응답 시간 | **3.125 ms** |
| 99th Percentile | 15.4 ms |
| 에러율 | 0% |

### 동시 예매 테스트 (재고 500, 요청 1,000)
| 지표 | 결과 |
|------|------|
| CONFIRMED 건수 | **500건** (목표: 정확히 500) |
| SOLD_OUT 건수 | 500건 |
| 데이터 정합성 | ✅ **일치** |
| 최종 DB remain_count | **0** (목표: 0) |

## 🏗️ 주요 기술적 의사결정 (Decision Log)

| 결정 사항 | 선택 이유 |
| :--- | :--- |
| **재고 관리 위치 (Redis Lua Script)** | DB UPDATE 락 경합으로 인한 TPS 급락 방지 및 원자성 보장 |
| **Kafka 비동기 처리** | 예매 이벤트 비동기 처리를 통해 API 응답 속도 개선 및 DB 부하 분산 |
| **PENDING 상태 존재** | Saga 패턴 준비 및 Kafka 처리 전 중간 상태 표현 |
| **KRaft 모드 (Kafka)** | Zookeeper 제거를 통한 로컬 환경 단순화 및 Kafka 3.x 표준 준수 |
| **open-in-view: false** | OSIV 비활성화를 통한 커넥션 풀 낭비 방지 및 실무 표준 준수 |

## 🚦 Getting Started


### Prerequisites
- Docker & Docker Compose
- Java 21

### Run Infrastructure
```bash
cd ticketing-system
docker compose up -d
```

### Run Application
```bash
./gradlew :ticketing-api:bootRun
```

## 📊 API Documentation
서버 구동 후 브라우저에서 아래 주소를 통해 확인 가능합니다.
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
