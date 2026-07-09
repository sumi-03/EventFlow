package com.example.eventflow.domain.reservation.dto;

import com.example.eventflow.domain.reservation.entity.Reservation;
import com.example.eventflow.domain.reservation.entity.ReservationStatus;
import com.example.eventflow.domain.seat.entity.Seat;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long reservationId,
        Long scheduleId,
        Long seatId,
        String seatNumber,
        Integer price,
        ReservationStatus status,
        LocalDateTime reservedAt
) {
    public static ReservationResponse from(Reservation reservation) {
        Seat seat = reservation.getSeat();
        return new ReservationResponse(
                reservation.getId(),
                seat.getEventSchedule().getId(),
                seat.getId(),
                seat.getSeatNumber(),
                reservation.getPrice(),
                reservation.getStatus(),
                reservation.getCreatedAt()
        );
    }
}
