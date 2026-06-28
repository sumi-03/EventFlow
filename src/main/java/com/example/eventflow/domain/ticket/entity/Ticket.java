package com.example.eventflow.domain.ticket.entity;

import com.example.eventflow.domain.event.entity.Event;
import com.example.eventflow.domain.reservation.entity.Reservation;
import com.example.eventflow.domain.user.entity.User;
import com.example.eventflow.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "tickets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, unique = true)
    private String qrToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    private LocalDateTime issuedAt;

    private LocalDateTime usedAt;

    public Ticket(Reservation reservation,
                  User user,
                  Event event,
                  String qrToken) {
        this.reservation = reservation;
        this.user = user;
        this.event = event;
        this.qrToken = qrToken;
        this.status = TicketStatus.ISSUED;
        this.issuedAt = LocalDateTime.now();
    }

    public void use() {
        this.status = TicketStatus.USED;
        this.usedAt = LocalDateTime.now();
    }
}
