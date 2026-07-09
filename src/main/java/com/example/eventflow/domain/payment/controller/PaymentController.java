package com.example.eventflow.domain.payment.controller;

import com.example.eventflow.domain.payment.dto.PaymentResponse;
import com.example.eventflow.domain.payment.service.PaymentService;
import com.example.eventflow.global.payload.CommonResponse;
import com.example.eventflow.global.payload.status.SuccessStatus;
import com.example.eventflow.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "결제", description = "예매 결제 API")
@RestController
@RequestMapping("/api/reservations/{reservationId}/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "결제", description = "예매를 결제 처리하고 예매 상태를 확정합니다.")
    @PostMapping
    public ResponseEntity<CommonResponse<PaymentResponse>> pay(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long reservationId) {
        PaymentResponse response = paymentService.pay(authUser.userId(), reservationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.of(SuccessStatus.PAYMENT_SUCCESS, response));
    }

    @Operation(summary = "결제 조회")
    @GetMapping
    public CommonResponse<PaymentResponse> getPayment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long reservationId) {
        return CommonResponse.onSuccess(paymentService.getPayment(authUser.userId(), reservationId));
    }
}
