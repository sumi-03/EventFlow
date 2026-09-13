package com.example.eventflow.domain.reservation.dto;

import jakarta.validation.constraints.NotNull;

public record ReservationCreateRequest(
        @NotNull Long seatId,
        // 대기열이 켜진 회차라면 필수. 대기열을 거치지 않은 회차면 무시된다
        String queueToken
) {
}
