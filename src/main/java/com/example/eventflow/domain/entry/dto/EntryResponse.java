package com.example.eventflow.domain.entry.dto;

import com.example.eventflow.domain.entry.entity.EntryLog;
import com.example.eventflow.domain.entry.entity.EntryResult;
import com.example.eventflow.domain.ticket.entity.Ticket;

import java.time.LocalDateTime;

// 입장 검증 결과 응답
public record EntryResponse(
        EntryResult result,
        Long ticketId,
        Long eventId,
        String eventTitle,
        String seatNumber,
        LocalDateTime checkedAt
) {
    public static EntryResponse from(EntryLog log, Ticket ticket) {
        return new EntryResponse(
                log.getResult(),
                ticket.getId(),
                ticket.getEvent().getId(),
                ticket.getEvent().getTitle(),
                ticket.getReservation().getSeat().getSeatNumber(),
                log.getCheckedAt()
        );
    }
}
