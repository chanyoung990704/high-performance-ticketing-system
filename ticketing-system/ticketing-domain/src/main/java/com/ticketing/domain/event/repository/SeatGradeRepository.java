package com.ticketing.domain.event.repository;

import com.ticketing.domain.event.SeatGrade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatGradeRepository extends JpaRepository<SeatGrade, Long> {
}
