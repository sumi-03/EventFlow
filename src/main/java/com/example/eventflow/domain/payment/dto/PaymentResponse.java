package com.example.eventflow.domain.payment.dto;

import com.example.eventflow.domain.payment.entity.Payment;
import com.example.eventflow.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long reservationId,
        Integer amount,
        PaymentStatus status,
        LocalDateTime paidAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReservation().getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaidAt()
        );
    }
}
