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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void relayEvents() {
        List<OutboxEvent> events = outboxRepository.findByStatus(OutboxStatus.INIT);
        if (events.isEmpty()) return;

        log.info("Relaying {} outbox events to Kafka...", events.size());

        for (OutboxEvent event : events) {
            try {
                JsonNode jsonNode = objectMapper.readTree(event.getPayload());
                String partitionKey = jsonNode.get("eventId").asText();

                kafkaTemplate.send("booking-created", partitionKey, event.getPayload());
                event.markAsSent();
            } catch (Exception e) {
                log.error("Failed to relay outbox event: id={}", event.getId(), e);
                event.markAsFailed();
            }
        }
    }
}
