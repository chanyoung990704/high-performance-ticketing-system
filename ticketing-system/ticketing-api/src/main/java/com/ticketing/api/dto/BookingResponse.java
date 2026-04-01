package com.ticketing.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long bookingId;
    private String status;

    public static BookingResponse pending(Long bookingId) {
        return new BookingResponse(bookingId, "PENDING");
    }
}
