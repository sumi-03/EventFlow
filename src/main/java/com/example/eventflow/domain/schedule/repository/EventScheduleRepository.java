package com.example.eventflow.domain.schedule.repository;

import com.example.eventflow.domain.schedule.entity.EventSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventScheduleRepository extends JpaRepository<EventSchedule, Long> {

    List<EventSchedule> findByEventIdOrderByStartAtAsc(Long eventId);
}
