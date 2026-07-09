package com.example.eventflow.domain.reservation.dto;

import jakarta.validation.constraints.NotNull;

public record ReservationCreateRequest(
        @NotNull Long seatId
) {
}
