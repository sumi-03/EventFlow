package com.example.eventflow.support;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public DatabaseCleaner(JdbcTemplate jdbcTemplate, RedisConnectionFactory redisConnectionFactory) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
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
        // TRUNCATE ... RESTART IDENTITY로 PK가 매번 1부터 다시 시작하므로,
        // 대기열 키(scheduleId 기준)가 이전 테스트 것과 겹치지 않도록 Redis도 같이 비운다
        redisConnectionFactory.getConnection().serverCommands().flushDb();
    }
}
