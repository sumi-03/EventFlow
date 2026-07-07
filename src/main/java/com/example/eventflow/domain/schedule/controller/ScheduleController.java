package com.example.eventflow.domain.schedule.controller;

import com.example.eventflow.domain.schedule.dto.ScheduleCreateRequest;
import com.example.eventflow.domain.schedule.dto.ScheduleResponse;
import com.example.eventflow.domain.schedule.service.ScheduleService;
import com.example.eventflow.global.payload.CommonResponse;
import com.example.eventflow.global.payload.status.SuccessStatus;
import com.example.eventflow.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "회차", description = "행사 회차 API")
@RestController
@RequestMapping("/api/events/{eventId}/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Operation(summary = "회차 생성")
    @PostMapping
    public ResponseEntity<CommonResponse<ScheduleResponse>> createSchedule(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long eventId,
            @Valid @RequestBody ScheduleCreateRequest request) {
        ScheduleResponse response = scheduleService.createSchedule(authUser.userId(), eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(SuccessStatus.SCHEDULE_CREATED, response));
    }

    @Operation(summary = "회차 목록 조회")
    @GetMapping
    public CommonResponse<List<ScheduleResponse>> getSchedules(@PathVariable Long eventId) {
        return CommonResponse.onSuccess(scheduleService.getSchedules(eventId));
    }
}
