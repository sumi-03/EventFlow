package com.example.eventflow.domain.schedule.dto;

import com.example.eventflow.domain.schedule.entity.EventSchedule;

import java.time.LocalDateTime;

public record ScheduleResponse(
        Long id,
        Long eventId,
        String name,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime saleStartAt,
        LocalDateTime saleEndAt
) {
    public static ScheduleResponse from(EventSchedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getEvent().getId(),
                schedule.getName(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getSaleStartAt(),
                schedule.getSaleEndAt()
        );
    }
}
