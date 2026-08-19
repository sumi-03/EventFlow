package com.example.eventflow.domain.ticket.service;

import com.example.eventflow.domain.reservation.entity.Reservation;
import com.example.eventflow.domain.ticket.dto.TicketResponse;
import com.example.eventflow.domain.ticket.entity.Ticket;
import com.example.eventflow.domain.ticket.repository.TicketRepository;
import com.example.eventflow.global.exception.BusinessException;
import com.example.eventflow.global.payload.status.ErrorStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public Ticket issue(Reservation reservation) {
        Ticket ticket = new Ticket(
                reservation,
                reservation.getUser(),
                reservation.getSeat().getEventSchedule().getEvent(),
                UUID.randomUUID().toString());
        return ticketRepository.save(ticket);
    }

    public List<TicketResponse> getMyTickets(Long userId) {
        return ticketRepository.findByUserIdOrderByIssuedAtDesc(userId).stream()
                .map(TicketResponse::from)
                .toList();
    }

    public TicketResponse getTicket(Long userId, Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.TICKET_NOT_FOUND));
        if (!ticket.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorStatus.TICKET_FORBIDDEN);
        }
        return TicketResponse.from(ticket);
    }
}
