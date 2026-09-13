package com.example.eventflow.domain.reservation.service;

import com.example.eventflow.domain.event.entity.Event;
import com.example.eventflow.domain.queue.service.AdmissionQueueService;
import com.example.eventflow.domain.reservation.dto.ReservationCreateRequest;
import com.example.eventflow.domain.reservation.dto.ReservationResponse;
import com.example.eventflow.domain.reservation.entity.Reservation;
import com.example.eventflow.domain.reservation.repository.ReservationRepository;
import com.example.eventflow.domain.schedule.entity.EventSchedule;
import com.example.eventflow.domain.seat.entity.Seat;
import com.example.eventflow.domain.seat.entity.SeatStatus;
import com.example.eventflow.domain.seat.repository.SeatRepository;
import com.example.eventflow.domain.user.entity.User;
import com.example.eventflow.domain.user.repository.UserRepository;
import com.example.eventflow.global.exception.BusinessException;
import com.example.eventflow.global.payload.status.ErrorStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final AdmissionQueueService admissionQueueService;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository,
                              SeatRepository seatRepository,
                              UserRepository userRepository,
                              AdmissionQueueService admissionQueueService,
                              Clock clock) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.admissionQueueService = admissionQueueService;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse reserve(Long userId, ReservationCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.USER_NOT_FOUND));
        Seat seat = seatRepository.findByIdForUpdate(request.seatId())
                .orElseThrow(() -> new BusinessException(ErrorStatus.SEAT_NOT_FOUND));

        validateReservable(seat);
        validateAdmitted(seat, request.queueToken());

        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new BusinessException(ErrorStatus.SEAT_ALREADY_RESERVED);
        }
        seat.reserve();

        Reservation reservation = new Reservation(user, seat, seat.getPrice());
        try {
            return ReservationResponse.from(reservationRepository.saveAndFlush(reservation));
        } catch (DataIntegrityViolationException e) {
            // 부분 유니크 인덱스 위반 = 락을 거치지 않은 경로에서 같은 좌석이 먼저 예약됨
            throw new BusinessException(ErrorStatus.SEAT_ALREADY_RESERVED);
        }
    }

    private void validateReservable(Seat seat) {
        EventSchedule schedule = seat.getEventSchedule();
        Event event = schedule.getEvent();
        LocalDateTime now = LocalDateTime.now(clock);

        if (!event.isOpen()) {
            throw new BusinessException(ErrorStatus.EVENT_CLOSED);
        }
        if (schedule.hasStartedAt(now)) {
            throw new BusinessException(ErrorStatus.SCHEDULE_ALREADY_STARTED);
        }
        if (schedule.isSaleNotStartedAt(now)) {
            throw new BusinessException(ErrorStatus.RESERVATION_SALE_NOT_STARTED);
        }
        if (schedule.isSaleEndedAt(now)) {
            throw new BusinessException(ErrorStatus.RESERVATION_SALE_ENDED);
        }
    }

    // 이 회차가 대기열을 거친 적 있다면(isQueueActive) permit 없이는 예매를 막는다.
    // 한 번도 대기열을 켠 적 없는 회차는 원래대로 대기열 없이 예매 가능
    private void validateAdmitted(Seat seat, String queueToken) {
        Long scheduleId = seat.getEventSchedule().getId();
        if (!admissionQueueService.isQueueActive(scheduleId)) {
            return;
        }
        if (!admissionQueueService.consumePermit(scheduleId, queueToken)) {
            throw new BusinessException(ErrorStatus.QUEUE_ADMISSION_REQUIRED);
        }
    }

    public List<ReservationResponse> getMyReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public ReservationResponse getReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.RESERVATION_NOT_FOUND));
        if (!reservation.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorStatus.RESERVATION_FORBIDDEN);
        }
        return ReservationResponse.from(reservation);
    }
}
