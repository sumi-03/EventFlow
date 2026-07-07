package com.example.eventflow.domain.schedule.controller;

import com.example.eventflow.domain.schedule.dto.ScheduleCreateRequest;
import com.example.eventflow.domain.schedule.dto.ScheduleResponse;
import com.example.eventflow.domain.schedule.service.ScheduleService;
import com.example.eventflow.global.payload.CommonResponse;
import com.example.eventflow.global.payload.status.SuccessStatus;
import com.example.eventflow.global.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ResponseEntity<CommonResponse<ScheduleResponse>> createSchedule(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long eventId,
            @Valid @RequestBody ScheduleCreateRequest request) {
        ScheduleResponse response = scheduleService.createSchedule(authUser.userId(), eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(SuccessStatus.SCHEDULE_CREATED, response));
    }

    @GetMapping
    public CommonResponse<List<ScheduleResponse>> getSchedules(@PathVariable Long eventId) {
        return CommonResponse.onSuccess(scheduleService.getSchedules(eventId));
    }
}
