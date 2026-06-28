package com.example.eventflow.domain.event.repository;

import com.example.eventflow.domain.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
