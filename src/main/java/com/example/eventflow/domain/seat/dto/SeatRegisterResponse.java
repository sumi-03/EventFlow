package com.example.eventflow.domain.seat.dto;

public record SeatRegisterResponse(
        Long scheduleId,
        int registeredCount
) {
}
