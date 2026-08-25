package com.example.eventflow.integration;

import com.example.eventflow.support.ApiFixture;
import com.example.eventflow.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HappyPathIntegrationTest extends IntegrationTestSupport {

    @Test
    void 행사생성부터_재입장거부까지_정상흐름() throws Exception {
        ApiFixture.Session organizer = api.signupAndLogin("organizer@test.com", "행사 운영자");
        ApiFixture.Session customer = api.signupAndLogin("customer@test.com", "관객");

        ApiFixture.Scenario scenario = api.createScenario(organizer, ApiFixture.Timeline.normal());

        Long reservationId = api.reserve(customer, scenario.seatId());
        api.pay(customer, reservationId);

        ApiFixture.TicketInfo ticket = api.firstTicket(customer);

        api.post(
                "/api/entries",
                organizer,
                Map.of("qrToken", ticket.qrToken())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.result").value("SUCCESS"));

        api.post(
                "/api/entries",
                organizer,
                Map.of("qrToken", ticket.qrToken())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.result").value("ALREADY_USED"));
    }
}
