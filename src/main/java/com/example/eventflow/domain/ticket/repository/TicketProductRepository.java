package com.example.eventflow.domain.ticket.repository;

import com.example.eventflow.domain.ticket.entity.TicketProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketProductRepository extends JpaRepository<TicketProduct, Long> {

    List<TicketProduct> findByEventId(Long eventId);
}
