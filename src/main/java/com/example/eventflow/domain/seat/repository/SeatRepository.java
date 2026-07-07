package com.example.eventflow.domain.seat.repository;

import com.example.eventflow.domain.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByEventScheduleIdOrderBySeatNumberAsc(Long scheduleId);

    @Query("select s.seatNumber from Seat s where s.eventSchedule.id = :scheduleId")
    List<String> findSeatNumbersByScheduleId(@Param("scheduleId") Long scheduleId);
}
