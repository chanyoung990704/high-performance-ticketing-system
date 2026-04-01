package com.ticketing.domain.booking.repository;

import com.ticketing.domain.booking.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}
