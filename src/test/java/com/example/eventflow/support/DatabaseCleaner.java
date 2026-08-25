package com.example.eventflow.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clear() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    entry_logs,
                    tickets,
                    payments,
                    reservations,
                    seats,
                    event_schedules,
                    events,
                    refresh_tokens,
                    users
                RESTART IDENTITY CASCADE
                """);
    }
}
