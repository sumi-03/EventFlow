package com.example.eventflow.domain.seat.dto;

import com.example.eventflow.domain.seat.entity.Seat;
import com.example.eventflow.domain.seat.entity.SeatGrade;
import com.example.eventflow.domain.seat.entity.SeatStatus;

public record SeatResponse(
        Long id,
        String seatNumber,
        SeatGrade grade,
        Integer price,
        SeatStatus status
) {
    public static SeatResponse from(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getGrade(),
                seat.getPrice(),
                seat.getStatus()
        );
    }
}
