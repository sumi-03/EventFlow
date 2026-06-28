package com.example.eventflow.domain.ticket.repository;

import com.example.eventflow.domain.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUserId(Long userId);

    Optional<Ticket> findByQrToken(String qrToken);
}
