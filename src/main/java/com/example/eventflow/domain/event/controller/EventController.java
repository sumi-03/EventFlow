package com.example.eventflow.domain.event.controller;

import com.example.eventflow.domain.event.dto.*;
import com.example.eventflow.domain.event.service.EventService;
import com.example.eventflow.global.payload.CommonResponse;
import com.example.eventflow.global.payload.status.SuccessStatus;
import com.example.eventflow.global.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<CommonResponse<EventResponse>> createEvent(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody EventCreateRequest request) {
        EventResponse response = eventService.createEvent(authUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(SuccessStatus.EVENT_CREATED, response));
    }

    @GetMapping
    public CommonResponse<EventListResponse> getEvents(
            @PageableDefault(size = 10, sort = "startAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return CommonResponse.onSuccess(eventService.getEvents(pageable));
    }

    @GetMapping("/{eventId}")
    public CommonResponse<EventResponse> getEvent(@PathVariable Long eventId) {
        return CommonResponse.onSuccess(eventService.getEvent(eventId));
    }

    @PatchMapping("/{eventId}")
    public CommonResponse<EventResponse> updateEvent(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long eventId,
            @Valid @RequestBody EventUpdateRequest request) {
        return CommonResponse.of(SuccessStatus.EVENT_UPDATED,
                eventService.updateEvent(authUser.userId(), eventId, request));
    }

    @PatchMapping("/{eventId}/close")
    public CommonResponse<EventResponse> closeEvent(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long eventId) {
        return CommonResponse.of(SuccessStatus.EVENT_CLOSED,
                eventService.closeEvent(authUser.userId(), eventId));
    }

    @DeleteMapping("/{eventId}")
    public CommonResponse<Void> deleteEvent(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long eventId) {
        eventService.deleteEvent(authUser.userId(), eventId);
        return CommonResponse.of(SuccessStatus.EVENT_DELETED, null);
    }
}
