package com.example.eventflow.domain.schedule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ScheduleCreateRequest(
        @Size(max = 100) String name,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @NotNull LocalDateTime saleStartAt,
        @NotNull LocalDateTime saleEndAt
) {
}
