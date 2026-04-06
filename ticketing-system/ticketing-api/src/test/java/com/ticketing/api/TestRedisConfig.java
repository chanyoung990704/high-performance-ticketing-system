package com.ticketing.api;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

@TestConfiguration
public class TestRedisConfig {

    private static RedisServer redisServer;

    static {
        // 스프링 컨텍스트 로드 전, 클래스 로딩 시점에 즉시 실행하여 
        // 다른 빈들이 Redis 연결을 시도하기 전에 서버를 준비합니다.
        try {
            redisServer = new RedisServer(6379);
            redisServer.start();
            System.out.println(">>> [STATIC] Embedded Redis started on port 6379");
        } catch (Exception e) {
            System.out.println(">>> [STATIC] Embedded Redis failed to start (might be already running): " + e.getMessage());
        }
    }

    @PreDestroy
    public void stopRedis() {
        // 테스트 전체 종료 시 JVM 종료와 함께 처리되도록 둠
    }
}
