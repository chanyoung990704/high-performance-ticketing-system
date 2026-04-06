package com.ticketing.api;

import com.ticketing.infra.redis.QueueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class RedisResilienceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QueueService queueService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    @DisplayName("시나리오 5: 오래된 대기열 사용자 자동 정리 검증")
    void testQueueCleanup() {
        // Given: 1시간 전 데이터를 대기열에 직접 삽입
        Long eventId = 1L;
        String queueKey = "queue:" + eventId;
        double oldScore = System.currentTimeMillis() - (60 * 60 * 1000L); // 1시간 전
        
        redisTemplate.opsForZSet().add(queueKey, "old_user", oldScore);
        redisTemplate.opsForZSet().add(queueKey, "new_user", (double) System.currentTimeMillis());

        // When: 청소 로직 실행
        queueService.cleanExpiredQueue(eventId);

        // Then: 오래된 사용자만 삭제되었는지 확인
        Boolean isOldUserPresent = redisTemplate.opsForZSet().score(queueKey, "old_user") != null;
        Boolean isNewUserPresent = redisTemplate.opsForZSet().score(queueKey, "new_user") != null;

        System.out.println(">>> [CLEANUP TEST] Old user present: " + isOldUserPresent);
        System.out.println(">>> [CLEANUP TEST] New user present: " + isNewUserPresent);

        assertThat(isOldUserPresent).isFalse();
        assertThat(isNewUserPresent).isTrue();
    }

    @Test
    @DisplayName("시나리오 6: Redis 모니터링 API(/info) 정상 응답 검증")
    void testRedisMonitorApi() throws Exception {
        mockMvc.perform(get("/api/v1/admin/redis/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.used_memory_human").exists())
                .andExpect(jsonPath("$.mem_fragmentation_ratio").exists());

        System.out.println(">>> [MONITOR API TEST] Redis info API responded with memory metrics.");
    }
}
