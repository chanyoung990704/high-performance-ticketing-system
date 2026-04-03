package com.ticketing.infra.outbox;

import com.ticketing.domain.outbox.OutboxEvent;
import com.ticketing.domain.outbox.OutboxRepository;
import com.ticketing.domain.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void relayEvents() {
        List<OutboxEvent> events = outboxRepository.findByStatus(OutboxStatus.INIT);
        if (events.isEmpty()) return;

        log.info("Relaying {} outbox events to Kafka...", events.size());

        for (OutboxEvent event : events) {
            try {
                // aggregateId를 키로 사용하여 순서 보장 (필요시)
                kafkaTemplate.send("booking-created", String.valueOf(event.getAggregateId()), event.getPayload());
                event.markAsSent();
            } catch (Exception e) {
                log.error("Failed to relay outbox event: id={}", event.getId(), e);
                event.markAsFailed();
            }
        }
    }
}
