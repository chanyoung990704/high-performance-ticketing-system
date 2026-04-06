package com.ticketing.api.consumer;

import com.ticketing.api.dto.BookingEvent;
import com.ticketing.domain.booking.BookingStatus;
import com.ticketing.domain.booking.repository.BookingRepository;
import com.ticketing.domain.event.repository.SeatGradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCreatedConsumer {

    private final BookingRepository bookingRepository;
    private final SeatGradeRepository seatGradeRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = "booking-created",
            groupId = "booking-processor",
            concurrency = "3"
    )
    @Transactional
    public void consume(String message, Acknowledgment ack) {
        try {
            BookingEvent event = objectMapper.readValue(message, BookingEvent.class);
            log.info("[Partition Processing] 예매 이벤트 수신: bookingId={}, userId={}", event.getBookingId(), event.getUserId());

            int updated = seatGradeRepository.decreaseRemainCount(event.getGradeId());
            
            if (updated == 0) {
                bookingRepository.updateStatus(event.getBookingId(), BookingStatus.CANCELLED);
                log.warn("DB 재고 부족으로 예매 취소: bookingId={}", event.getBookingId());
            } else {
                bookingRepository.updateStatus(event.getBookingId(), BookingStatus.CONFIRMED);
                log.info("예매 확정 완료: bookingId={}", event.getBookingId());
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("예매 처리 중 치명적 오류 발생, DLQ로 전송: message={}", message, e);
            kafkaTemplate.send("booking-created.DLQ", message);
            ack.acknowledge();
        }
    }
}
