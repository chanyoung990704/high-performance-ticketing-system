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

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCreatedConsumer {

    private final BookingRepository bookingRepository;
    private final SeatGradeRepository seatGradeRepository;

    @KafkaListener(topics = "booking-created", groupId = "booking-processor")
    @Transactional
    public void consume(BookingEvent event) {
        log.info("예매 이벤트 수신: bookingId={}, userId={}", event.getBookingId(), event.getUserId());

        try {
            // 1. DB 재고 차감 (벌크 UPDATE로 lost update 방지)
            int updated = seatGradeRepository.decreaseRemainCount(event.getGradeId());
            
            if (updated == 0) {
                // 재고 부족 -> CANCELLED 처리
                bookingRepository.updateStatus(event.getBookingId(), BookingStatus.CANCELLED);
                log.warn("DB 재고 부족으로 예매 취소: bookingId={}", event.getBookingId());
                return;
            }

            // 2. 예매 확정
            bookingRepository.updateStatus(event.getBookingId(), BookingStatus.CONFIRMED);
            log.info("예매 확정 완료: bookingId={}", event.getBookingId());

        } catch (Exception e) {
            log.error("예매 처리 중 오류 발생: bookingId={}", event.getBookingId(), e);
            // 재시도를 위해 예외를 다시 던질 수 있음 (Kafka의 Ack 전략에 따라 다름)
            throw e; 
        }
    }
}
