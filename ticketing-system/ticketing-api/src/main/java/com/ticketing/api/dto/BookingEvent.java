package com.ticketing.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class BookingEvent {
    private Long bookingId;
    private Long userId;
    private Long eventId;
    private Long gradeId;
}
