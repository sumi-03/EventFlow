package com.example.eventflow.domain.seat.dto;

import com.example.eventflow.domain.seat.entity.Seat;
import com.example.eventflow.domain.seat.entity.SeatStatus;

import java.util.List;

public record SeatListResponse(
        Long scheduleId,
        int totalSeats,
        long availableSeats,
        List<SeatResponse> seats
) {
    public static SeatListResponse from(Long scheduleId, List<Seat> seats) {
        long available = seats.stream()
                .filter(seat -> seat.getStatus() == SeatStatus.AVAILABLE)
                .count();
        return new SeatListResponse(
                scheduleId,
                seats.size(),
                available,
                seats.stream().map(SeatResponse::from).toList()
        );
    }
}
