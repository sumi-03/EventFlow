package com.example.eventflow.domain.event.controller;

import com.example.eventflow.domain.event.dto.*;
import com.example.eventflow.domain.event.service.EventService;
import com.example.eventflow.global.payload.CommonResponse;
import com.example.eventflow.global.payload.status.SuccessStatus;
import com.example.eventflow.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "행사", description = "행사 CRUD API")
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "행사 생성")
    @PostMapping
    public ResponseEntity<CommonResponse<EventResponse>> createEvent(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody EventCreateRequest request) {
        EventResponse response = eventService.createEvent(authUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(SuccessStatus.EVENT_CREATED, response));
    }

    @Operation(summary = "행사 목록 조회")
    @GetMapping
    public CommonResponse<EventListResponse> getEvents(
            @ParameterObject @PageableDefault(size = 10, sort = "startAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return CommonResponse.onSuccess(eventService.getEvents(pageable));
    }

    @Operation(summary = "행사 상세 조회")
    @GetMapping("/{eventId}")
    public CommonResponse<EventResponse> getEvent(@PathVariable Long eventId) {
        return CommonResponse.onSuccess(eventService.getEvent(eventId));
    }

    @Operation(summary = "행사 수정")
    @PatchMapping("/{eventId}")
    public CommonResponse<EventResponse> updateEvent(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long eventId,
            @Valid @RequestBody EventUpdateRequest request) {
        return CommonResponse.of(SuccessStatus.EVENT_UPDATED,
                eventService.updateEvent(authUser.userId(), eventId, request));
    }

    @Operation(summary = "행사 마감", description = "행사를 CLOSED 상태로 변경합니다.")
    @PatchMapping("/{eventId}/close")
    public CommonResponse<EventResponse> closeEvent(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long eventId) {
        return CommonResponse.of(SuccessStatus.EVENT_CLOSED,
                eventService.closeEvent(authUser.userId(), eventId));
    }

    @Operation(summary = "행사 삭제")
    @DeleteMapping("/{eventId}")
    public CommonResponse<Void> deleteEvent(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long eventId) {
        eventService.deleteEvent(authUser.userId(), eventId);
        return CommonResponse.of(SuccessStatus.EVENT_DELETED, null);
    }
}
