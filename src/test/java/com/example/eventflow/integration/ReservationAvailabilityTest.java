package com.example.eventflow.integration;

import com.example.eventflow.support.ApiFixture;
import com.example.eventflow.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservationAvailabilityTest extends IntegrationTestSupport {

    @Test
    void 판매_시작_전에는_예매할_수_없다() throws Exception {
        var organizer = api.signupAndLogin("organizer@test.com", "운영자");
        var customer = api.signupAndLogin("customer@test.com", "관객");

        var beforeSale = new ApiFixture.Timeline(
                LocalDateTime.of(2026, 9, 1, 19, 0),
                LocalDateTime.of(2026, 9, 1, 22, 0),
                LocalDateTime.of(2026, 9, 1, 19, 0),
                LocalDateTime.of(2026, 9, 1, 22, 0),
                LocalDateTime.of(2026, 8, 21, 9, 0),
                LocalDateTime.of(2026, 8, 31, 23, 0)
        );

        var scenario = api.createScenario(organizer, beforeSale);

        api.post(
                "/api/reservations",
                customer,
                Map.of("seatId", scenario.seatId())
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION4091"));
    }

    @Test
    void 판매_종료_후에는_예매할_수_없다() throws Exception {
        var organizer = api.signupAndLogin("organizer@test.com", "운영자");
        var customer = api.signupAndLogin("customer@test.com", "관객");

        var afterSale = new ApiFixture.Timeline(
                LocalDateTime.of(2026, 9, 1, 19, 0),
                LocalDateTime.of(2026, 9, 1, 22, 0),
                LocalDateTime.of(2026, 9, 1, 19, 0),
                LocalDateTime.of(2026, 9, 1, 22, 0),
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 19, 23, 0)
        );

        var scenario = api.createScenario(organizer, afterSale);

        api.post(
                "/api/reservations",
                customer,
                Map.of("seatId", scenario.seatId())
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION4092"));
    }

    @Test
    void 마감된_행사는_예매할_수_없다() throws Exception {
        var organizer = api.signupAndLogin("organizer@test.com", "운영자");
        var customer = api.signupAndLogin("customer@test.com", "관객");

        var scenario = api.createScenario(organizer, ApiFixture.Timeline.normal());

        api.patch(
                "/api/events/{eventId}/close",
                organizer,
                null,
                scenario.eventId()
        ).andExpect(status().isOk());

        api.post(
                "/api/reservations",
                customer,
                Map.of("seatId", scenario.seatId())
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT409"));
    }

    @Test
    void 이미_시작된_공연은_예매할_수_없다() throws Exception {
        var organizer = api.signupAndLogin("organizer@test.com", "운영자");
        var customer = api.signupAndLogin("customer@test.com", "관객");

        var alreadyStarted = new ApiFixture.Timeline(
                LocalDateTime.of(2026, 8, 19, 10, 0),
                LocalDateTime.of(2026, 8, 19, 22, 0),
                LocalDateTime.of(2026, 8, 19, 10, 0),
                LocalDateTime.of(2026, 8, 19, 22, 0),
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 19, 9, 0)
        );

        var scenario = api.createScenario(organizer, alreadyStarted);

        api.post(
                "/api/reservations",
                customer,
                Map.of("seatId", scenario.seatId())
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE409"));
    }

    @Test
    void 다른_사람의_예약은_결제할_수_없다() throws Exception {
        var organizer = api.signupAndLogin("organizer@test.com", "운영자");
        var customer = api.signupAndLogin("customer@test.com", "관객");
        var attacker = api.signupAndLogin("attacker@test.com", "공격자");

        var scenario = api.createScenario(organizer, ApiFixture.Timeline.normal());
        Long reservationId = api.reserve(customer, scenario.seatId());

        api.post(
                "/api/reservations/{reservationId}/payment",
                attacker,
                null,
                reservationId
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION403"));
    }
}
