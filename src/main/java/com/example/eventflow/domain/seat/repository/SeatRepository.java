package com.example.eventflow.domain.seat.repository;

import com.example.eventflow.domain.seat.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByEventScheduleIdOrderBySeatNumberAsc(Long scheduleId);

    @Query("select s.seatNumber from Seat s where s.eventSchedule.id = :scheduleId")
    List<String> findSeatNumbersByScheduleId(@Param("scheduleId") Long scheduleId);

    // 좌석 행에 쓰기 잠금을 걸어 동시 예매 요청을 직렬화
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id = :id")
    Optional<Seat> findByIdForUpdate(@Param("id") Long id);
}
