package com.example.eventflow.domain.entry.entity;

import com.example.eventflow.domain.event.entity.Event;
import com.example.eventflow.domain.ticket.entity.Ticket;
import com.example.eventflow.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "entry_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntryLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EntryResult result;

    private LocalDateTime checkedAt;

    public EntryLog(Ticket ticket,
                    Event event,
                    EntryResult result) {
        this.ticket = ticket;
        this.event = event;
        this.result = result;
        this.checkedAt = LocalDateTime.now();
    }
}
