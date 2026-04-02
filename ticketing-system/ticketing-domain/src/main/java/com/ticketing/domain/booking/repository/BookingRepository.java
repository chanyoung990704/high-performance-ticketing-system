package com.ticketing.domain.booking.repository;

import com.ticketing.domain.booking.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ticketing.domain.booking.BookingStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Modifying
    @Query("UPDATE Booking b SET b.status = :status WHERE b.id = :bookingId")
    void updateStatus(@Param("bookingId") Long bookingId, @Param("status") BookingStatus status);
}
