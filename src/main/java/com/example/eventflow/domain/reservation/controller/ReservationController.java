package com.example.eventflow.domain.reservation.controller;

import com.example.eventflow.domain.reservation.dto.ReservationCreateRequest;
import com.example.eventflow.domain.reservation.dto.ReservationResponse;
import com.example.eventflow.domain.reservation.service.ReservationService;
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

@Tag(name = "예매", description = "좌석 예매 API")
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "좌석 예매", description = "좌석을 지정해 예매합니다.")
    @PostMapping
    public ResponseEntity<CommonResponse<ReservationResponse>> reserve(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ReservationCreateRequest request) {
        ReservationResponse response = reservationService.reserve(authUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(SuccessStatus.RESERVATION_CREATED, response));
    }

    @Operation(summary = "내 예매 목록 조회")
    @GetMapping
    public CommonResponse<List<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal AuthUser authUser) {
        return CommonResponse.onSuccess(reservationService.getMyReservations(authUser.userId()));
    }

    @Operation(summary = "예매 상세 조회")
    @GetMapping("/{reservationId}")
    public CommonResponse<ReservationResponse> getReservation(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long reservationId) {
        return CommonResponse.onSuccess(reservationService.getReservation(authUser.userId(), reservationId));
    }
}
