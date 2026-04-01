package com.ticketing.api;

import com.ticketing.domain.dto.QueueStatusResponse;
import com.ticketing.infra.redis.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class QueueServiceTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final Long eventId = 1L;

    @BeforeEach
    void cleanUp() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("대기열 진입 및 순위 조회 테스트")
    void enterQueueAndRankTest() {
        // given
        Long userA = 101L;
        Long userB = 102L;

        // when
        queueService.enterQueue(eventId, userA);
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        queueService.enterQueue(eventId, userB);

        // then
        QueueStatusResponse statusA = queueService.getStatus(eventId, userA);
        QueueStatusResponse statusB = queueService.getStatus(eventId, userB);

        assertThat(statusA.getRank()).isEqualTo(1L);
        assertThat(statusB.getRank()).isEqualTo(2L);
    }

    @Test
    @DisplayName("대기열에서 활성 토큰으로 전환 테스트")
    void processQueueTest() {
        // given
        for (long i = 1; i <= 5; i++) {
            queueService.enterQueue(eventId, i);
        }

        // when
        queueService.processQueue(eventId, 3); // 상위 3명 통과

        // then
        assertThat(queueService.getStatus(eventId, 1L).getStatus()).isEqualTo("ADMITTED");
        assertThat(queueService.getStatus(eventId, 3L).getStatus()).isEqualTo("ADMITTED");
        assertThat(queueService.getStatus(eventId, 4L).getStatus()).isEqualTo("WAITING");
        assertThat(queueService.getStatus(eventId, 4L).getRank()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Lua Script 기반 동시 재고 감소 테스트 - 10개 재고에 30명 요청")
    void concurrencyStockTest() throws Exception {
        // given
        Long gradeId = 1L;
        int initialStock = 10;
        int threadCount = 30;
        queueService.setStock(eventId, gradeId, initialStock);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        CompletableFuture<?>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                boolean success = queueService.decreaseStock(eventId, gradeId);
                if (success) successCount.incrementAndGet();
                else failCount.incrementAndGet();
            }, executorService);
        }

        CompletableFuture.allOf(futures).join();

        // then
        assertThat(successCount.get()).isEqualTo(initialStock);
        assertThat(failCount.get()).isEqualTo(threadCount - initialStock);

        String remain = redisTemplate.opsForValue().get(String.format("remain:%d:%d", eventId, gradeId));
        assertThat(Integer.parseInt(Objects.requireNonNull(remain))).isEqualTo(0);
    }
}
