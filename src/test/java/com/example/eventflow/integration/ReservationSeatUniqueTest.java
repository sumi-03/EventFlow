package com.example.eventflow.integration;

import com.example.eventflow.domain.reservation.entity.Reservation;
import com.example.eventflow.domain.reservation.repository.ReservationRepository;
import com.example.eventflow.domain.seat.entity.Seat;
import com.example.eventflow.domain.seat.repository.SeatRepository;
import com.example.eventflow.domain.user.entity.User;
import com.example.eventflow.domain.user.repository.UserRepository;
import com.example.eventflow.support.ApiFixture;
import com.example.eventflow.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationSeatUniqueTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void 같은_좌석에_활성_예약이_있으면_DB가_두_번째_예약을_거부한다() throws Exception {
        ApiFixture.Session organizer = api.signupAndLogin("organizer@test.com", "운영자");
        ApiFixture.Scenario scenario = api.createScenario(organizer, ApiFixture.Timeline.normal());
        Seat seat = seatRepository.findById(scenario.seatId()).orElseThrow();
        User a = userRepository.save(new User("a@test.com", "x", "관객A"));
        User b = userRepository.save(new User("b@test.com", "x", "관객B"));

        reservationRepository.saveAndFlush(new Reservation(a, seat, seat.getPrice()));

        assertThatThrownBy(() ->
                reservationRepository.saveAndFlush(new Reservation(b, seat, seat.getPrice())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 기존_예약이_취소된_좌석은_다시_예약할_수_있다() throws Exception {
        ApiFixture.Session organizer = api.signupAndLogin("organizer@test.com", "운영자");
        ApiFixture.Scenario scenario = api.createScenario(organizer, ApiFixture.Timeline.normal());
        Seat seat = seatRepository.findById(scenario.seatId()).orElseThrow();
        User a = userRepository.save(new User("a@test.com", "x", "관객A"));
        User b = userRepository.save(new User("b@test.com", "x", "관객B"));

        Reservation first = reservationRepository.saveAndFlush(new Reservation(a, seat, seat.getPrice()));
        first.cancel();
        reservationRepository.saveAndFlush(first);

        assertThatCode(() ->
                reservationRepository.saveAndFlush(new Reservation(b, seat, seat.getPrice())))
                .doesNotThrowAnyException();
    }
}
