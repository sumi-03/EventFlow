package com.example.eventflow.domain.event.dto;

import com.example.eventflow.domain.event.entity.Event;
import com.example.eventflow.domain.event.entity.EventStatus;

import java.time.LocalDateTime;

// 목록 조회용 경량 응답 (생성자 정보 제외 → N+1 회피)
public record EventSummaryResponse(
        Long id,
        String title,
        String location,
        LocalDateTime startAt,
        LocalDateTime endAt,
        EventStatus status
) {
    public static EventSummaryResponse from(Event event) {
        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getLocation(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus()
        );
    }
}
