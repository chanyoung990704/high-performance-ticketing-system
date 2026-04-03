package com.ticketing.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {
    private Long userId;
    private Long eventId;
    private Long gradeId;
}
