package com.ticketing.api.service;

import com.ticketing.api.dto.BookingEvent;
import com.ticketing.api.dto.BookingRequest;
import com.ticketing.api.dto.BookingResponse;
import com.ticketing.api.exception.DuplicateBookingException;
import com.ticketing.api.exception.NotAdmittedException;
import com.ticketing.api.exception.NotFoundException;
import com.ticketing.api.exception.SoldOutException;
import com.ticketing.domain.booking.Booking;
import com.ticketing.domain.booking.BookingStatus;
import com.ticketing.domain.booking.repository.BookingRepository;
import com.ticketing.domain.event.SeatGrade;
import com.ticketing.domain.event.repository.SeatGradeRepository;
import com.ticketing.infra.redis.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final QueueService queueService;
    private final BookingRepository bookingRepository;
    private final SeatGradeRepository seatGradeRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public BookingResponse createBooking(Long userId, BookingRequest request) {
        Long eventId = request.getEventId();
        Long gradeId = request.getGradeId();

        // 1. 입장 허가 확인
        if (!queueService.isAdmitted(eventId, userId)) {
            throw new NotAdmittedException("대기열 입장 허가가 필요합니다.");
        }

        // 2. 중복 예매 확인
        if (queueService.isAlreadyBooked(eventId, userId)) {
            throw new DuplicateBookingException("이미 예매하셨습니다.");
        }

        // 3. Redis 재고 감소 (원자적)
        boolean stockDecreased = queueService.decreaseStock(eventId, gradeId);
        if (!stockDecreased) {
            throw new SoldOutException("매진되었습니다.");
        }

        // 4. PENDING 상태로 예매 레코드 저장
        SeatGrade seatGrade = seatGradeRepository.findById(gradeId)
                .orElseThrow(() -> new NotFoundException("좌석 등급이 없습니다."));

        Booking booking = new Booking(userId, seatGrade, BookingStatus.PENDING, seatGrade.getPrice());
        bookingRepository.save(booking);

        // 5. Kafka 이벤트 발행 (비동기 처리를 위함)
        kafkaTemplate.send("booking-created", String.valueOf(userId),
                BookingEvent.of(booking.getId(), userId, eventId, gradeId));

        // 6. 중복 예매 방지 Set에 추가
        queueService.markAsBooked(eventId, userId);

        return BookingResponse.pending(booking.getId());
    }

    @Transactional(readOnly = true)
    public Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("예약 정보를 찾을 수 없습니다."));
    }
}
