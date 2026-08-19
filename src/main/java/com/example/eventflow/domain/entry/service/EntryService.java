package com.example.eventflow.domain.entry.service;

import com.example.eventflow.domain.entry.dto.EntryRequest;
import com.example.eventflow.domain.entry.dto.EntryResponse;
import com.example.eventflow.domain.entry.entity.EntryLog;
import com.example.eventflow.domain.entry.entity.EntryResult;
import com.example.eventflow.domain.entry.repository.EntryLogRepository;
import com.example.eventflow.domain.ticket.entity.Ticket;
import com.example.eventflow.domain.ticket.repository.TicketRepository;
import com.example.eventflow.global.exception.BusinessException;
import com.example.eventflow.global.payload.status.ErrorStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EntryService {

    private final TicketRepository ticketRepository;
    private final EntryLogRepository entryLogRepository;

    public EntryService(TicketRepository ticketRepository, EntryLogRepository entryLogRepository) {
        this.ticketRepository = ticketRepository;
        this.entryLogRepository = entryLogRepository;
    }

    // QR 토큰으로 티켓 상태를 확인해 입장 처리하고 결과를 로그로 남김
    @Transactional
    public EntryResponse verify(EntryRequest request) {
        Ticket ticket = ticketRepository.findByQrToken(request.qrToken())
                .orElseThrow(() -> new BusinessException(ErrorStatus.TICKET_NOT_FOUND));

        EntryResult result = resolve(ticket);
        if (result == EntryResult.SUCCESS) {
            ticket.use();
        }

        EntryLog log = entryLogRepository.save(
                new EntryLog(ticket, ticket.getEvent(), result));
        return EntryResponse.from(log, ticket);
    }

    private EntryResult resolve(Ticket ticket) {
        return switch (ticket.getStatus()) {
            case ISSUED -> EntryResult.SUCCESS;
            case USED -> EntryResult.ALREADY_USED;
            case CANCELLED -> EntryResult.INVALID;
        };
    }
}
