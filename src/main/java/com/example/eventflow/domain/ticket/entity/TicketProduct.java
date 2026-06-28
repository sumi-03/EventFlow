package com.example.eventflow.domain.ticket.entity;

import com.example.eventflow.domain.event.entity.Event;
import com.example.eventflow.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "ticket_products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer remainingQuantity;

    @Column(nullable = false)
    private LocalDateTime saleStartAt;

    @Column(nullable = false)
    private LocalDateTime saleEndAt;

    public TicketProduct(Event event, String name, Integer price, Integer totalQuantity,
                         LocalDateTime saleStartAt, LocalDateTime saleEndAt) {
        this.event = event;
        this.name = name;
        this.price = price;
        this.totalQuantity = totalQuantity;
        this.remainingQuantity = totalQuantity;
        this.saleStartAt = saleStartAt;
        this.saleEndAt = saleEndAt;
    }

    public void decreaseStock(int quantity) {
        if (remainingQuantity < quantity) {
            throw new IllegalArgumentException("남은 티켓 수량이 부족합니다.");
        }
        this.remainingQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        this.remainingQuantity += quantity;
    }
}
