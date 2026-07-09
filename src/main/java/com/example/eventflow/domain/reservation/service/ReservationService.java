package com.example.eventflow.domain.reservation.service;

import com.example.eventflow.domain.reservation.dto.ReservationCreateRequest;
import com.example.eventflow.domain.reservation.dto.ReservationResponse;
import com.example.eventflow.domain.reservation.entity.Reservation;
import com.example.eventflow.domain.reservation.repository.ReservationRepository;
import com.example.eventflow.domain.seat.entity.Seat;
import com.example.eventflow.domain.seat.entity.SeatStatus;
import com.example.eventflow.domain.seat.repository.SeatRepository;
import com.example.eventflow.domain.user.entity.User;
import com.example.eventflow.domain.user.repository.UserRepository;
import com.example.eventflow.global.exception.BusinessException;
import com.example.eventflow.global.payload.status.ErrorStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              SeatRepository seatRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReservationResponse reserve(Long userId, ReservationCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.USER_NOT_FOUND));
        Seat seat = seatRepository.findById(request.seatId())
                .orElseThrow(() -> new BusinessException(ErrorStatus.SEAT_NOT_FOUND));

        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new BusinessException(ErrorStatus.SEAT_ALREADY_RESERVED);
        }
        seat.reserve();

        Reservation reservation = new Reservation(user, seat, seat.getPrice());
        return ReservationResponse.from(reservationRepository.save(reservation));
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
