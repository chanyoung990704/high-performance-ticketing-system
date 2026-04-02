package com.ticketing.api.dto;

import com.ticketing.domain.booking.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long bookingId;
    private String status;
    private Integer price;
    private Long seatGradeId;
    private LocalDateTime bookedAt;

    public static BookingResponse pending(Long bookingId) {
        return BookingResponse.builder()
                .bookingId(bookingId)
                .status("PENDING")
                .build();
    }

    public static BookingResponse from(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .status(booking.getStatus().name())
                .price(booking.getPrice())
                .seatGradeId(booking.getSeatGrade().getId())
                .bookedAt(booking.getBookedAt())
                .build();
    }
}
