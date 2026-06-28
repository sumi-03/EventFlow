package com.example.eventflow.domain.payment.entity;

import com.example.eventflow.domain.reservation.entity.Reservation;
import com.example.eventflow.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @Column(nullable = false)
    private Integer amount;

    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    private LocalDateTime paidAt;

    public Payment(Reservation reservation, Integer amount) {
        this.reservation = reservation;
        this.amount = amount;
        this.status = PaymentStatus.READY;
    }

    public void success(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.SUCCESS;
        this.paidAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }
}
