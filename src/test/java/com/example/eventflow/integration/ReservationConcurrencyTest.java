package com.example.eventflow.integration;

import com.example.eventflow.domain.reservation.dto.ReservationCreateRequest;
import com.example.eventflow.domain.reservation.repository.ReservationRepository;
import com.example.eventflow.domain.reservation.service.ReservationService;
import com.example.eventflow.domain.user.entity.User;
import com.example.eventflow.domain.user.repository.UserRepository;
import com.example.eventflow.support.ApiFixture;
import com.example.eventflow.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationConcurrencyTest extends IntegrationTestSupport {

    private static final int CONCURRENT_REQUESTS = 100;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void 동일_좌석에_동시에_예매해도_예약은_한_건만_생성된다() throws Exception {
        // 1. 행사·회차·좌석 1개 준비
        ApiFixture.Session organizer = api.signupAndLogin("organizer@test.com", "운영자");
        ApiFixture.Scenario scenario = api.createScenario(organizer, ApiFixture.Timeline.normal());
        Long seatId = scenario.seatId();

        // 2. 이 좌석 하나를 노리는 관객 100명 생성
        List<Long> customerIds = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            User customer = userRepository.save(
                    new User("customer" + i + "@test.com", "unused", "관객" + i));
            customerIds.add(customer.getId());
        }

        // 3. 관객마다 "같은 좌석을 예매하는" 스레드를 하나씩 만든다
        List<Thread> threads = new ArrayList<>();
        for (Long customerId : customerIds) {
            Thread thread = new Thread(() -> {
                try {
                    reservationService.reserve(customerId, new ReservationCreateRequest(seatId, null));
                } catch (Exception e) {
                    // 예매 실패(좌석이 이미 선점됨 등)는 예상된 결과이므로 무-시한다
                }
            });
            threads.add(thread);
        }

        // 4. 100개를 최대한 동시에 출발시키고, 전부 끝날 때까지 기다린다
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        // 5. 좌석은 1개인데 예약이 몇 건 생겼는지 DB에 직접 물어본다
        long reservationCount = reservationRepository.countBySeatId(seatId);
        System.out.println("========================================");
        System.out.println(" 좌석 수            : 1");
        System.out.println(" 동시 예매 시도자 수 : " + CONCURRENT_REQUESTS);
        System.out.println(" 실제 생성된 예약 건수: " + reservationCount);
        System.out.println("========================================");

        // 비관적 락으로 요청이 좌석 행 단위로 직렬화되어 예약은 1건만 생성
        assertThat(reservationCount).isEqualTo(1);
    }
}
