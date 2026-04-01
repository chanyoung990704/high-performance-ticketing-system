?? 1주차 목표 요약
영역	목표	완료 기준
프로젝트 셋업	Docker 환경 + 멀티모듈 구조	docker compose up 원클릭 실행
도메인 설계	ERD + API 명세	테이블 5개 이상, Notion 문서화
Redis 대기열	선착순 쿠폰 발급 핵심 로직	동시 1,000명 테스트 통과
성능 측정	JMeter 기초 시나리오	TPS 수치 README에 기록
?? 일별 상세 계획
Day 1 (월) ? 프로젝트 셋업 & 환경 구성
예상 소요 시간: 3~4시간

① GitHub 레포 & 멀티모듈 구조 생성
text
ticketing-system/
├── ticketing-api/          # 메인 API 서버 (Spring Boot)
├── ticketing-domain/       # 도메인 엔티티, Repository
├── ticketing-infra/        # Redis, Kafka 설정, 외부 인프라 연동
└── docker-compose.yml      # 전체 환경 정의
ticketing-api가 ticketing-domain, ticketing-infra 의존

Gradle 멀티프로젝트 설정 (settings.gradle, 각 모듈 build.gradle)

② docker-compose.yml 작성
text
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: ticketing
      MYSQL_ROOT_PASSWORD: root1234
    ports: ["3306:3306"]
    volumes: ["./docker/mysql/init.sql:/docker-entrypoint-initdb.d/init.sql"]

  redis:
    image: redis:7.2-alpine
    ports: ["6379:6379"]
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    environment:
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_NODE_ID: 1
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      CLUSTER_ID: "MkU3OEVBNTcwNTJENDM2Qk"  # KRaft 모드 (Zookeeper 불필요)
    ports: ["9092:9092"]

  redis-insight:
    image: redis/redisinsight:latest
    ports: ["5540:5540"]   # Redis GUI
?? KRaft 모드 사용 이유: Zookeeper 없이 Kafka 단독 실행 → 로컬 개발 환경 단순화

③ application.yml 기본 설정
text
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ticketing?useSSL=false&serverTimezone=Asia/Seoul
    username: root
    password: root1234
  jpa:
    hibernate.ddl-auto: validate   # Flyway 사용 예정이므로 validate
    show-sql: true
    open-in-view: false             # OSIV 비활성화 (성능 최적화)
  data.redis:
    host: localhost
    port: 6379
  kafka:
    bootstrap-servers: localhost:9092
    consumer.group-id: ticketing-group

management:
  endpoints.web.exposure.include: "*"   # Actuator 전체 노출
  endpoint.health.show-details: always
? Day 1 완료 체크리스트
GitHub 레포 생성 및 브랜치 전략 설정 (main, develop, feature/*)

멀티모듈 Gradle 빌드 성공 (./gradlew build)

docker compose up 실행 후 MySQL, Redis, Kafka 모두 정상 기동 확인

Redis Insight에서 localhost:5540 접속 확인
