package com.ticketing.api.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.api.dto.BookingEvent;
import com.ticketing.domain.booking.BookingStatus;
import com.ticketing.domain.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingDlqConsumer {

    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "booking-created.DLQ", groupId = "dlq-processor")
    @Transactional
    public void consumeDlq(String message) {
        try {
            BookingEvent event = objectMapper.readValue(message, BookingEvent.class);
            log.error("[DLQ ALERT] Manual intervention required: bookingId={}, userId={}, eventId={}",
                    event.getBookingId(), event.getUserId(), event.getEventId());

            bookingRepository.updateStatus(event.getBookingId(), BookingStatus.FAILED);

        } catch (Exception e) {
            log.error("Failed to process DLQ message: {}", message, e);
        }
    }
}
