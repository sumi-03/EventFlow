package com.example.eventflow.domain.seat.controller;

import com.example.eventflow.domain.seat.dto.SeatListResponse;
import com.example.eventflow.domain.seat.dto.SeatRegisterRequest;
import com.example.eventflow.domain.seat.dto.SeatRegisterResponse;
import com.example.eventflow.domain.seat.service.SeatService;
import com.example.eventflow.global.payload.CommonResponse;
import com.example.eventflow.global.payload.status.SuccessStatus;
import com.example.eventflow.global.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedules/{scheduleId}/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping
    public ResponseEntity<CommonResponse<SeatRegisterResponse>> registerSeats(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long scheduleId,
            @Valid @RequestBody SeatRegisterRequest request) {
        SeatRegisterResponse response = seatService.registerSeats(authUser.userId(), scheduleId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(SuccessStatus.SEAT_REGISTERED, response));
    }

    @GetMapping
    public CommonResponse<SeatListResponse> getSeats(@PathVariable Long scheduleId) {
        return CommonResponse.onSuccess(seatService.getSeats(scheduleId));
    }
}
