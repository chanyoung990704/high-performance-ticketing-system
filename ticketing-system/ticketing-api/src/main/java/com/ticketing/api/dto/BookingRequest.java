package com.ticketing.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BookingRequest {
    private Long eventId;
    private Long gradeId;
}
