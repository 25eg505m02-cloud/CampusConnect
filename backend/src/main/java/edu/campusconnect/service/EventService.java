package edu.campusconnect.service;

import edu.campusconnect.dto.ApiDtos.EventRequest;
import edu.campusconnect.dto.ApiDtos.EventResponse;
import edu.campusconnect.entity.Event;
import edu.campusconnect.entity.EventRegistration;
import edu.campusconnect.entity.Role;
import edu.campusconnect.entity.User;
import edu.campusconnect.exception.ApiException;
import edu.campusconnect.repository.EventRegistrationRepository;
import edu.campusconnect.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class EventService {
    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final EntityLookup lookup;
    private final NotificationService notifications;

    public EventService(EventRepository events, EventRegistrationRepository registrations,
                        EntityLookup lookup, NotificationService notifications) {
        this.events = events;
        this.registrations = registrations;
        this.lookup = lookup;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> list(Pageable pageable) {
        return events.findAllByOrderByStartAtAsc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse get(Long id) {
        return toResponse(lookup.event(id));
    }

    @Transactional
    public EventResponse create(String email, EventRequest request) {
        User creator = lookup.userByEmail(email);
        requireEventManager(creator);
        validateDates(request.startAt(), request.endAt());
        Event event = new Event();
        apply(event, request);
        event.setCreator(creator);
        return toResponse(events.save(event));
    }

    @Transactional
    public EventResponse update(String email, Long eventId, EventRequest request) {
        User user = lookup.userByEmail(email);
        Event event = lookup.event(eventId);
        if (!event.getCreator().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Only the event creator or an administrator may edit this event.");
        }
        validateDates(request.startAt(), request.endAt());
        if (request.capacity() != null && request.capacity() < registrations.countByEventId(eventId)) {
            throw ApiException.conflict("Event capacity cannot be lower than the number of current registrations.");
        }
        apply(event, request);
        return toResponse(event);
    }

    @Transactional
    public void delete(String email, Long eventId) {
        User user = lookup.userByEmail(email);
        Event event = lookup.event(eventId);
        if (!event.getCreator().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Only the event creator or an administrator may delete this event.");
        }
        if (registrations.countByEventId(eventId) > 0) {
            throw ApiException.conflict("Events with registrations cannot be deleted.");
        }
        events.delete(event);
    }

    @Transactional
    public void register(String email, Long eventId) {
        User user = lookup.userByEmail(email);
        Event event = events.findByIdForUpdate(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found."));
        if (registrations.existsByEventIdAndUserId(eventId, user.getId())) {
            throw ApiException.conflict("You are already registered for this event.");
        }
        long registeredCount = registrations.countByEventId(eventId);
        if (event.getCapacity() != null && registeredCount >= event.getCapacity()) {
            throw ApiException.conflict("This event has reached its registration capacity.");
        }
        registrations.save(new EventRegistration(event, user));
        if (!event.getCreator().getId().equals(user.getId())) {
            notifications.notify(event.getCreator(), "EVENT_REGISTRATION",
                    user.getFullName() + " registered for " + event.getTitle() + ".", eventId);
        }
    }

    @Transactional
    public void unregister(String email, Long eventId) {
        User user = lookup.userByEmail(email);
        lookup.event(eventId);
        if (!registrations.existsByEventIdAndUserId(eventId, user.getId())) {
            throw ApiException.notFound("You are not registered for this event.");
        }
        registrations.deleteByEventIdAndUserId(eventId, user.getId());
    }

    private void requireEventManager(User user) {
        if (user.getRole() != Role.FACULTY && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Only faculty and administrators can manage events.");
        }
    }

    private void validateDates(Instant startAt, Instant endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw ApiException.badRequest("Event endAt must be after startAt.");
        }
    }

    private void apply(Event event, EventRequest request) {
        event.setTitle(request.title().trim());
        event.setDescription(request.description().trim());
        event.setVenue(request.venue().trim());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setCapacity(request.capacity());
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(event.getId(), event.getTitle(), event.getDescription(), event.getVenue(),
                event.getStartAt(), event.getEndAt(), event.getCapacity(),
                registrations.countByEventId(event.getId()), event.getCreator().getId(),
                event.getCreator().getFullName(), event.getCreatedAt());
    }
}
