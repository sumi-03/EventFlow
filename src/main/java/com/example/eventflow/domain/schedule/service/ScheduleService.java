package com.example.eventflow.domain.schedule.service;

import com.example.eventflow.domain.event.entity.Event;
import com.example.eventflow.domain.event.repository.EventRepository;
import com.example.eventflow.domain.schedule.dto.ScheduleCreateRequest;
import com.example.eventflow.domain.schedule.dto.ScheduleResponse;
import com.example.eventflow.domain.schedule.entity.EventSchedule;
import com.example.eventflow.domain.schedule.repository.EventScheduleRepository;
import com.example.eventflow.global.exception.BusinessException;
import com.example.eventflow.global.payload.status.ErrorStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ScheduleService {

    private final EventScheduleRepository scheduleRepository;
    private final EventRepository eventRepository;

    public ScheduleService(EventScheduleRepository scheduleRepository, EventRepository eventRepository) {
        this.scheduleRepository = scheduleRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public ScheduleResponse createSchedule(Long userId, Long eventId, ScheduleCreateRequest request) {
        validatePeriod(request);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.EVENT_NOT_FOUND));
        if (!event.isOwnedBy(userId)) {
            throw new BusinessException(ErrorStatus.EVENT_FORBIDDEN);
        }
        EventSchedule schedule = new EventSchedule(
                event, request.name(), request.startAt(), request.endAt(),
                request.saleStartAt(), request.saleEndAt());
        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    public List<ScheduleResponse> getSchedules(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new BusinessException(ErrorStatus.EVENT_NOT_FOUND);
        }
        return scheduleRepository.findByEventIdOrderByStartAtAsc(eventId).stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    private void validatePeriod(ScheduleCreateRequest request) {
        LocalDateTime start = request.startAt();
        LocalDateTime end = request.endAt();
        LocalDateTime saleStart = request.saleStartAt();
        LocalDateTime saleEnd = request.saleEndAt();
        boolean showValid = end.isAfter(start);
        boolean saleValid = saleEnd.isAfter(saleStart);
        boolean saleBeforeShow = !saleStart.isAfter(start);
        if (!showValid || !saleValid || !saleBeforeShow) {
            throw new BusinessException(ErrorStatus.INVALID_SCHEDULE_PERIOD);
        }
    }
}
