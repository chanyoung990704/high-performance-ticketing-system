package com.ticketing.domain.event.repository;

import com.ticketing.domain.event.SeatGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatGradeRepository extends JpaRepository<SeatGrade, Long> {

    @Modifying
    @Query("UPDATE SeatGrade sg SET sg.remainCount = sg.remainCount - 1 " +
           "WHERE sg.id = :gradeId AND sg.remainCount > 0")
    int decreaseRemainCount(@Param("gradeId") Long gradeId);
}
