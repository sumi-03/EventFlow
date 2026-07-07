package com.example.eventflow.domain.schedule.entity;

import com.example.eventflow.domain.event.entity.Event;
import com.example.eventflow.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 행사의 특정 일시 공연(회차)
@Entity
@Getter
@Table(name = "event_schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(length = 100)
    private String name;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private LocalDateTime saleStartAt;

    @Column(nullable = false)
    private LocalDateTime saleEndAt;

    public EventSchedule(Event event, String name, LocalDateTime startAt, LocalDateTime endAt,
                         LocalDateTime saleStartAt, LocalDateTime saleEndAt) {
        this.event = event;
        this.name = name;
        this.startAt = startAt;
        this.endAt = endAt;
        this.saleStartAt = saleStartAt;
        this.saleEndAt = saleEndAt;
    }

    public boolean isOwnedBy(Long userId) {
        return event.isOwnedBy(userId);
    }
}
