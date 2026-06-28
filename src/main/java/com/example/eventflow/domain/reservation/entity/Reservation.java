package com.example.eventflow.domain.reservation.entity;

import com.example.eventflow.domain.ticket.entity.TicketProduct;
import com.example.eventflow.domain.user.entity.User;
import com.example.eventflow.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_product_id", nullable = false)
    private TicketProduct ticketProduct;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    public Reservation(User user,
                       TicketProduct ticketProduct,
                       Integer quantity,
                       Integer totalPrice) {
        this.user = user;
        this.ticketProduct = ticketProduct;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = ReservationStatus.PENDING;
    }

    public void completePayment() {
        this.status = ReservationStatus.PAID;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }
}
