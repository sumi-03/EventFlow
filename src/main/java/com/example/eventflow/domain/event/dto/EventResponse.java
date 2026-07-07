package com.example.eventflow.domain.event.dto;

import com.example.eventflow.domain.event.entity.Event;
import com.example.eventflow.domain.event.entity.EventStatus;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String title,
        String description,
        String location,
        LocalDateTime startAt,
        LocalDateTime endAt,
        EventStatus status,
        Long creatorId,
        String creatorName,
        LocalDateTime createdAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus(),
                event.getCreatedBy().getId(),
                event.getCreatedBy().getName(),
                event.getCreatedAt()
        );
    }
}
