package com.ticketing.infra.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {

    private final QueueService queueService;

    public RedisKeyExpirationListener(RedisMessageListenerContainer container, QueueService queueService) {
        super(container);
        this.queueService = queueService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.info("Redis 키 만료 이벤트 수신: {}", expiredKey);

        if (expiredKey.startsWith("active:")) {
            try {
                String eventIdStr = expiredKey.replace("active:", "");
                Long eventId = Long.parseLong(eventIdStr);
                log.info("활성 토큰 만료 감지 (이벤트: {}). 다음 대기자 입장 처리를 시작합니다.", eventId);
                queueService.processQueue(eventId, 10);
            } catch (Exception e) {
                log.error("만료 토큰 재처리 중 오류 발생: {}", expiredKey, e);
            }
        }
    }
}
