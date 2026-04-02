package com.ticketing.infra.redis;

import com.ticketing.domain.dto.QueueStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final StringRedisTemplate redisTemplate;

    private static final String QUEUE_KEY = "queue:%d";
    private static final String REMAIN_KEY = "remain:%d:%d";
    private static final String ACTIVE_KEY = "active:%d";
    private static final String BOOKED_KEY = "booked:%d";
    private static final int ACTIVE_TOKEN_TTL = 300; // 5분

    private static final String DECREASE_STOCK_SCRIPT = 
            "local remain = tonumber(redis.call('GET', KEYS[1])) " +
            "if remain == nil or remain <= 0 then " +
            "    return 0 " +
            "end " +
            "redis.call('DECR', KEYS[1]) " +
            "return 1";

    /**
     * 대기열 진입
     */
    public long enterQueue(Long eventId, Long userId) {
        String key = String.format(QUEUE_KEY, eventId);
        double score = System.currentTimeMillis();

        redisTemplate.opsForZSet().addIfAbsent(key, String.valueOf(userId), score);
        Long rank = redisTemplate.opsForZSet().rank(key, String.valueOf(userId));
        return rank != null ? rank : -1;
    }

    /**
     * 모든 활성 이벤트의 대기열을 1초마다 처리 (테스트용 자동 승인)
     */
    @Scheduled(fixedDelay = 1000)
    public void scheduleQueueProcess() {
        Set<String> keys = redisTemplate.keys("queue:*");
        if (keys == null) return;

        for (String key : keys) {
            try {
                Long eventId = Long.parseLong(key.split(":")[1]);
                processQueue(eventId, 10); // 한 번에 10명씩 승인
            } catch (Exception e) {
                log.error("Failed to process queue for key: {}", key, e);
            }
        }
    }

    /**
     * 대기열 처리 (스케줄러 호출용)
     */
    public void processQueue(Long eventId, int batchSize) {
        String queueKey = String.format(QUEUE_KEY, eventId);
        String activeKey = String.format(ACTIVE_KEY, eventId);

        Set<String> candidates = redisTemplate.opsForZSet().range(queueKey, 0, batchSize - 1);
        if (candidates == null || candidates.isEmpty()) return;

        for (String userId : candidates) {
            redisTemplate.opsForSet().add(activeKey, userId);
            redisTemplate.opsForZSet().remove(queueKey, userId);
        }
        redisTemplate.expire(activeKey, Duration.ofSeconds(ACTIVE_TOKEN_TTL));
    }

    /**
     * 대기 순위 및 상태 조회
     */
    public QueueStatusResponse getStatus(Long eventId, Long userId) {
        String queueKey = String.format(QUEUE_KEY, eventId);
        String activeKey = String.format(ACTIVE_KEY, eventId);

        Boolean isActive = redisTemplate.opsForSet().isMember(activeKey, String.valueOf(userId));
        if (Boolean.TRUE.equals(isActive)) {
            return QueueStatusResponse.admitted();
        }

        Long rank = redisTemplate.opsForZSet().rank(queueKey, String.valueOf(userId));
        if (rank != null) {
            long estimatedWaitSeconds = rank * 1L; // 1명당 1초 가정
            return QueueStatusResponse.waiting(rank + 1, estimatedWaitSeconds);
        }

        return QueueStatusResponse.notFound();
    }

    /**
     * 입장 허가 여부 확인
     */
    public boolean isAdmitted(Long eventId, Long userId) {
        String activeKey = String.format(ACTIVE_KEY, eventId);
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(activeKey, String.valueOf(userId)));
    }

    /**
     * 중복 예매 여부 확인
     */
    public boolean isAlreadyBooked(Long eventId, Long userId) {
        String bookedKey = String.format(BOOKED_KEY, eventId);
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(bookedKey, String.valueOf(userId)));
    }

    /**
     * 예매 완료 처리 (중복 방지 세트 추가)
     */
    public void markAsBooked(Long eventId, Long userId) {
        String bookedKey = String.format(BOOKED_KEY, eventId);
        redisTemplate.opsForSet().add(bookedKey, String.valueOf(userId));
    }

    /**
     * Lua Script 기반 원자적 재고 감소
     */
    public boolean decreaseStock(Long eventId, Long gradeId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(DECREASE_STOCK_SCRIPT, Long.class);
        String key = String.format(REMAIN_KEY, eventId, gradeId);
        Long result = redisTemplate.execute(script, Collections.singletonList(key));
        return Long.valueOf(1L).equals(result);
    }

    /**
     * 테스트용 재고 초기 설정
     */
    public void setStock(Long eventId, Long gradeId, int count) {
        String key = String.format(REMAIN_KEY, eventId, gradeId);
        redisTemplate.opsForValue().set(key, String.valueOf(count));
    }
}
