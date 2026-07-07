package com.example.eventflow.domain.event.service;

import com.example.eventflow.domain.event.dto.*;
import com.example.eventflow.domain.event.entity.Event;
import com.example.eventflow.domain.event.repository.EventRepository;
import com.example.eventflow.domain.user.entity.User;
import com.example.eventflow.domain.user.repository.UserRepository;
import com.example.eventflow.global.exception.BusinessException;
import com.example.eventflow.global.payload.status.ErrorStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EventResponse createEvent(Long userId, EventCreateRequest request) {
        validatePeriod(request.startAt(), request.endAt());
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.USER_NOT_FOUND));
        Event event = new Event(
                request.title(), request.description(), request.location(),
                request.startAt(), request.endAt(), creator);
        return EventResponse.from(eventRepository.save(event));
    }

    public EventListResponse getEvents(Pageable pageable) {
        return EventListResponse.from(eventRepository.findAll(pageable));
    }

    public EventResponse getEvent(Long eventId) {
        return EventResponse.from(findEvent(eventId));
    }

    @Transactional
    public EventResponse updateEvent(Long userId, Long eventId, EventUpdateRequest request) {
        validatePeriod(request.startAt(), request.endAt());
        Event event = findOwnedEvent(userId, eventId);
        event.update(request.title(), request.description(), request.location(),
                request.startAt(), request.endAt());
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse closeEvent(Long userId, Long eventId) {
        Event event = findOwnedEvent(userId, eventId);
        event.close();
        return EventResponse.from(event);
    }

    @Transactional
    public void deleteEvent(Long userId, Long eventId) {
        Event event = findOwnedEvent(userId, eventId);
        eventRepository.delete(event);
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.EVENT_NOT_FOUND));
    }

    private Event findOwnedEvent(Long userId, Long eventId) {
        Event event = findEvent(eventId);
        if (!event.isOwnedBy(userId)) {
            throw new BusinessException(ErrorStatus.EVENT_FORBIDDEN);
        }
        return event;
    }

    private void validatePeriod(java.time.LocalDateTime startAt, java.time.LocalDateTime endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new BusinessException(ErrorStatus.INVALID_EVENT_PERIOD);
        }
    }
}
