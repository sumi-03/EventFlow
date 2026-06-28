package com.example.eventflow.domain.entry.repository;

import com.example.eventflow.domain.entry.entity.EntryLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryLogRepository extends JpaRepository<EntryLog, Long> {
}
