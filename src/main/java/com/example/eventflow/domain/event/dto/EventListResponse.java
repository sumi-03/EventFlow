package com.example.eventflow.domain.event.dto;

import com.example.eventflow.domain.event.entity.Event;
import org.springframework.data.domain.Page;

import java.util.List;

public record EventListResponse(
        List<EventSummaryResponse> events,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static EventListResponse from(Page<Event> page) {
        return new EventListResponse(
                page.getContent().stream().map(EventSummaryResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
