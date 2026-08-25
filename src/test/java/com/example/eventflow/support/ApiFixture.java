package com.example.eventflow.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ApiFixture {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public ApiFixture(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public record Session(Long userId, String accessToken) {
    }

    public record Scenario(Long eventId, Long scheduleId, Long seatId) {
    }

    public record TicketInfo(Long ticketId, String qrToken) {
    }

    public record Timeline(
            LocalDateTime eventStartAt,
            LocalDateTime eventEndAt,
            LocalDateTime scheduleStartAt,
            LocalDateTime scheduleEndAt,
            LocalDateTime saleStartAt,
            LocalDateTime saleEndAt
    ) {
        public static Timeline normal() {
            return new Timeline(
                    LocalDateTime.of(2026, 9, 1, 19, 0),
                    LocalDateTime.of(2026, 9, 1, 22, 0),
                    LocalDateTime.of(2026, 9, 1, 19, 0),
                    LocalDateTime.of(2026, 9, 1, 22, 0),
                    LocalDateTime.of(2026, 8, 19, 9, 0),
                    LocalDateTime.of(2026, 8, 31, 23, 0)
            );
        }
    }

    public Session signupAndLogin(String email, String name) throws Exception {
        MvcResult signup = post(
                "/api/auth/signup",
                null,
                Map.of(
                        "email", email,
                        "password", "password1234",
                        "name", name
                )
        )
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult login = post(
                "/api/auth/login",
                null,
                Map.of(
                        "email", email,
                        "password", "password1234"
                )
        )
                .andExpect(status().isOk())
                .andReturn();

        Long userId = json(signup).at("/result/id").longValue();
        String token = json(login).at("/result/accessToken").textValue();

        return new Session(userId, token);
    }

    public Scenario createScenario(Session organizer, Timeline timeline) throws Exception {
        MvcResult eventResult = post(
                "/api/events",
                organizer,
                Map.of(
                        "title", "EventFlow 테스트 공연",
                        "description", "통합 테스트용 행사",
                        "location", "테스트 공연장",
                        "startAt", timeline.eventStartAt().toString(),
                        "endAt", timeline.eventEndAt().toString()
                )
        )
                .andExpect(status().isCreated())
                .andReturn();

        Long eventId = json(eventResult).at("/result/id").longValue();

        MvcResult scheduleResult = post(
                "/api/events/{eventId}/schedules",
                organizer,
                Map.of(
                        "name", "1회차",
                        "startAt", timeline.scheduleStartAt().toString(),
                        "endAt", timeline.scheduleEndAt().toString(),
                        "saleStartAt", timeline.saleStartAt().toString(),
                        "saleEndAt", timeline.saleEndAt().toString()
                ),
                eventId
        )
                .andExpect(status().isCreated())
                .andReturn();

        Long scheduleId = json(scheduleResult).at("/result/id").longValue();

        post(
                "/api/schedules/{scheduleId}/seats",
                organizer,
                Map.of(
                        "groups", List.of(
                                Map.of(
                                        "grade", "VIP",
                                        "price", 100000,
                                        "rowPrefix", "A",
                                        "startNumber", 1,
                                        "endNumber", 3
                                )
                        )
                ),
                scheduleId
        ).andExpect(status().isCreated());

        MvcResult seatsResult = get(
                "/api/schedules/{scheduleId}/seats",
                null,
                scheduleId
        )
                .andExpect(status().isOk())
                .andReturn();

        Long seatId = json(seatsResult).at("/result/seats/0/id").longValue();

        return new Scenario(eventId, scheduleId, seatId);
    }

    public Long reserve(Session customer, Long seatId) throws Exception {
        MvcResult result = post(
                "/api/reservations",
                customer,
                Map.of("seatId", seatId)
        )
                .andExpect(status().isCreated())
                .andReturn();

        return json(result).at("/result/reservationId").longValue();
    }

    public void pay(Session customer, Long reservationId) throws Exception {
        post(
                "/api/reservations/{reservationId}/payment",
                customer,
                null,
                reservationId
        ).andExpect(status().isCreated());
    }

    public TicketInfo firstTicket(Session customer) throws Exception {
        MvcResult result = get("/api/tickets", customer)
                .andExpect(status().isOk())
                .andReturn();

        JsonNode ticket = json(result).at("/result/0");

        return new TicketInfo(
                ticket.path("ticketId").longValue(),
                ticket.path("qrToken").textValue()
        );
    }

    public ResultActions post(String uri, Session session, Object body, Object... uriVariables) throws Exception {
        var request = MockMvcRequestBuilders
                .post(uri, uriVariables)
                .contentType(MediaType.APPLICATION_JSON);

        if (session != null) {
            request.header(AUTHORIZATION, "Bearer " + session.accessToken());
        }

        if (body != null) {
            request.content(objectMapper.writeValueAsString(body));
        }

        return mockMvc.perform(request);
    }

    public ResultActions get(String uri, Session session, Object... uriVariables) throws Exception {
        var request = MockMvcRequestBuilders.get(uri, uriVariables);

        if (session != null) {
            request.header(AUTHORIZATION, "Bearer " + session.accessToken());
        }

        return mockMvc.perform(request);
    }

    public ResultActions patch(String uri, Session session, Object body, Object... uriVariables) throws Exception {
        var request = MockMvcRequestBuilders
                .patch(uri, uriVariables)
                .contentType(MediaType.APPLICATION_JSON);

        if (session != null) {
            request.header(AUTHORIZATION, "Bearer " + session.accessToken());
        }

        if (body != null) {
            request.content(objectMapper.writeValueAsString(body));
        }

        return mockMvc.perform(request);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
