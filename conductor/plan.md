# 구현 계획: 1주차 1일차 - 프로젝트 구조 잡기 및 환경 설정

## 목표
멀티 모듈 구조의 티켓팅 시스템을 초기화하고, Docker를 이용한 개발 환경(MySQL, Redis, Kafka)을 구축합니다.

## 주요 파일 및 컨텍스트
- `ticketing-system/` (루트 디렉토리)
- `ticketing-api/` (API 모듈)
- `ticketing-domain/` (도메인 및 엔티티 모듈)
- `ticketing-infra/` (인프라 및 외부 연동 모듈)
- `build.gradle`, `settings.gradle`
- `docker-compose.yml`
- `application.yml`

## 상세 구현 단계

### 1. 프로젝트 구조 생성
- `ticketing-system/` 루트 폴더 생성.
- 하위 모듈 디렉토리 생성: `ticketing-api/`, `ticketing-domain/`, `ticketing-infra/`.

### 2. Gradle 멀티 모듈 설정
- `settings.gradle` 작성:
  ```gradle
  rootProject.name = 'ticketing-system'
  include 'ticketing-api'
  include 'ticketing-domain'
  include 'ticketing-infra'
  ```
- 루트 및 각 모듈별 `build.gradle` 작성 (Spring Boot, JPA, Redis, Kafka 의존성 추가).

### 3. Docker 환경 구성
- `docker-compose.yml` 작성:
  - MySQL 8.0 (ticketing DB, root1234)
  - Redis 7.2-alpine (256MB LRU)
  - Kafka 7.6.0 (KRaft 모드)
  - Redis Insight (GUI)

### 4. 애플리케이션 설정
- `ticketing-api/src/main/resources/application.yml` 작성:
  - 데이터베이스, Redis, Kafka 연결 정보.
  - Actuator 엔드포인트 노출 설정.

## 검증 및 테스트
1. **Docker 실행**: `docker compose up -d` 명령어로 모든 컨테이너가 정상 구동되는지 확인 (`docker ps`).
2. **빌드 확인**: `./gradlew build` 명령어로 전체 모듈 빌드 성공 여부 확인.
3. **연결 확인**: MySQL 및 Redis 접속 확인 (로그 또는 Redis Insight 이용).
4. **Kafka 상태**: 로그를 통해 KRaft 모드로 정상 동작하는지 확인.
