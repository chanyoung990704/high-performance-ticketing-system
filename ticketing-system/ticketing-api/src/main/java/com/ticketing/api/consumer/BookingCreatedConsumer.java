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

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCreatedConsumer {

    private final BookingRepository bookingRepository;
    private final SeatGradeRepository seatGradeRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "booking-created", groupId = "booking-processor")
    @Transactional
    public void consume(String message) {
        try {
            BookingEvent event = objectMapper.readValue(message, BookingEvent.class);
            log.info("예매 이벤트 수신: bookingId={}, userId={}", event.getBookingId(), event.getUserId());

            // 1. DB 재고 차감
            int updated = seatGradeRepository.decreaseRemainCount(event.getGradeId());
            
            if (updated == 0) {
                bookingRepository.updateStatus(event.getBookingId(), BookingStatus.CANCELLED);
                log.warn("DB 재고 부족으로 예매 취소: bookingId={}", event.getBookingId());
                return;
            }

            // 2. 예매 확정
            bookingRepository.updateStatus(event.getBookingId(), BookingStatus.CONFIRMED);
            log.info("예매 확정 완료: bookingId={}", event.getBookingId());

        } catch (Exception e) {
            log.error("예매 처리 중 오류 발생: message={}", message, e);
            throw new RuntimeException(e); 
        }
    }
}
