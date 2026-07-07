package com.example.eventflow.domain.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventUpdateRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @Size(max = 255) String location,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt
) {
}
