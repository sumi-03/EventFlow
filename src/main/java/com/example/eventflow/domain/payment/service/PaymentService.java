package com.example.eventflow.domain.payment.service;

import com.example.eventflow.domain.payment.dto.PaymentResponse;
import com.example.eventflow.domain.payment.entity.Payment;
import com.example.eventflow.domain.payment.repository.PaymentRepository;
import com.example.eventflow.domain.reservation.entity.Reservation;
import com.example.eventflow.domain.reservation.entity.ReservationStatus;
import com.example.eventflow.domain.reservation.repository.ReservationRepository;
import com.example.eventflow.global.exception.BusinessException;
import com.example.eventflow.global.payload.status.ErrorStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          ReservationRepository reservationRepository) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public PaymentResponse pay(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.RESERVATION_NOT_FOUND));
        if (!reservation.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorStatus.RESERVATION_FORBIDDEN);
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException(ErrorStatus.PAYMENT_NOT_ALLOWED);
        }

        Payment payment = new Payment(reservation, reservation.getPrice());
        payment.success("MOCK-" + UUID.randomUUID());
        reservation.completePayment();
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    public PaymentResponse getPayment(Long userId, Long reservationId) {
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.PAYMENT_NOT_FOUND));
        if (!payment.getReservation().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorStatus.RESERVATION_FORBIDDEN);
        }
        return PaymentResponse.from(payment);
    }
}
