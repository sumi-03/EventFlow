package com.example.eventflow.domain.seat.service;

import com.example.eventflow.domain.schedule.entity.EventSchedule;
import com.example.eventflow.domain.schedule.repository.EventScheduleRepository;
import com.example.eventflow.domain.seat.dto.SeatListResponse;
import com.example.eventflow.domain.seat.dto.SeatRegisterRequest;
import com.example.eventflow.domain.seat.dto.SeatRegisterResponse;
import com.example.eventflow.domain.seat.entity.Seat;
import com.example.eventflow.domain.seat.repository.SeatRepository;
import com.example.eventflow.global.exception.BusinessException;
import com.example.eventflow.global.payload.status.ErrorStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class SeatService {

    private final SeatRepository seatRepository;
    private final EventScheduleRepository scheduleRepository;

    public SeatService(SeatRepository seatRepository, EventScheduleRepository scheduleRepository) {
        this.seatRepository = seatRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional
    public SeatRegisterResponse registerSeats(Long userId, Long scheduleId, SeatRegisterRequest request) {
        EventSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.SCHEDULE_NOT_FOUND));
        if (!schedule.isOwnedBy(userId)) {
            throw new BusinessException(ErrorStatus.EVENT_FORBIDDEN);
        }

        Set<String> seatNumbers = new HashSet<>(seatRepository.findSeatNumbersByScheduleId(scheduleId));
        List<Seat> seats = new ArrayList<>();
        for (SeatRegisterRequest.SeatGroup group : request.groups()) {
            if (group.endNumber() < group.startNumber()) {
                throw new BusinessException(ErrorStatus.INVALID_SEAT_RANGE);
            }
            for (int n = group.startNumber(); n <= group.endNumber(); n++) {
                String seatNumber = group.rowPrefix() + n;
                if (!seatNumbers.add(seatNumber)) {
                    throw new BusinessException(ErrorStatus.DUPLICATE_SEAT);
                }
                seats.add(new Seat(schedule, seatNumber, group.grade(), group.price()));
            }
        }
        seatRepository.saveAll(seats);
        return new SeatRegisterResponse(scheduleId, seats.size());
    }

    public SeatListResponse getSeats(Long scheduleId) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new BusinessException(ErrorStatus.SCHEDULE_NOT_FOUND);
        }
        return SeatListResponse.from(scheduleId,
                seatRepository.findByEventScheduleIdOrderBySeatNumberAsc(scheduleId));
    }
}
