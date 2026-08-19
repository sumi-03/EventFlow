package com.example.eventflow.domain.ticket.dto;

import com.example.eventflow.domain.seat.entity.Seat;
import com.example.eventflow.domain.ticket.entity.Ticket;
import com.example.eventflow.domain.ticket.entity.TicketStatus;

import java.time.LocalDateTime;

public record TicketResponse(
        Long ticketId,
        Long reservationId,
        Long eventId,
        String eventTitle,
        Long scheduleId,
        String seatNumber,
        String qrToken,
        TicketStatus status,
        LocalDateTime issuedAt
) {
    public static TicketResponse from(Ticket ticket) {
        Seat seat = ticket.getReservation().getSeat();
        return new TicketResponse(
                ticket.getId(),
                ticket.getReservation().getId(),
                ticket.getEvent().getId(),
                ticket.getEvent().getTitle(),
                seat.getEventSchedule().getId(),
                seat.getSeatNumber(),
                ticket.getQrToken(),
                ticket.getStatus(),
                ticket.getIssuedAt()
        );
    }
}
