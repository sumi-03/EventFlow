package com.example.eventflow.domain.seat.controller;

import com.example.eventflow.domain.seat.dto.SeatListResponse;
import com.example.eventflow.domain.seat.dto.SeatRegisterRequest;
import com.example.eventflow.domain.seat.dto.SeatRegisterResponse;
import com.example.eventflow.domain.seat.service.SeatService;
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

@Tag(name = "좌석", description = "회차 좌석 API")
@RestController
@RequestMapping("/api/schedules/{scheduleId}/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @Operation(summary = "좌석 일괄 등록", description = "구역 단위로 좌석을 일괄 생성합니다.")
    @PostMapping
    public ResponseEntity<CommonResponse<SeatRegisterResponse>> registerSeats(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long scheduleId,
            @Valid @RequestBody SeatRegisterRequest request) {
        SeatRegisterResponse response = seatService.registerSeats(authUser.userId(), scheduleId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(SuccessStatus.SEAT_REGISTERED, response));
    }

    @Operation(summary = "좌석 목록 조회")
    @GetMapping
    public CommonResponse<SeatListResponse> getSeats(@PathVariable Long scheduleId) {
        return CommonResponse.onSuccess(seatService.getSeats(scheduleId));
    }
}
