package com.example.eventflow.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
public class TestTimeConfig {

    @Bean
    @Primary
    public Clock testClock() {
        return Clock.fixed(
                Instant.parse("2026-08-20T00:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
    }
}
