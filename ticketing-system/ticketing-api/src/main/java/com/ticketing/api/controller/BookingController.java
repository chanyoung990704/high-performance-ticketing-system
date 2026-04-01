package com.ticketing.api.controller;

import com.ticketing.api.dto.BookingRequest;
import com.ticketing.api.dto.BookingResponse;
import com.ticketing.api.service.BookingService;
import com.ticketing.domain.booking.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public BookingResponse createBooking(@RequestParam Long userId, @RequestBody BookingRequest request) {
        return bookingService.createBooking(userId, request);
    }

    @GetMapping("/{bookingId}")
    public Booking getBooking(@PathVariable Long bookingId) {
        return bookingService.getBooking(bookingId);
    }
}
