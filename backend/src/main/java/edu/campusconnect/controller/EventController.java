package edu.campusconnect.controller;

import edu.campusconnect.dto.ApiDtos.EventRequest;
import edu.campusconnect.dto.ApiDtos.EventResponse;
import edu.campusconnect.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {
    private final EventService events;

    public EventController(EventService events) {
        this.events = events;
    }

    @GetMapping
    public Page<EventResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return events.list(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(
            @AuthenticationPrincipal UserDetails currentUser, @Valid @RequestBody EventRequest request) {
        return events.create(currentUser.getUsername(), request);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return events.get(id);
    }

    @PatchMapping("/{id}")
    public EventResponse update(
            @AuthenticationPrincipal UserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request) {
        return events.update(currentUser.getUsername(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        events.delete(currentUser.getUsername(), id);
    }

    @PostMapping("/{id}/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        events.register(currentUser.getUsername(), id);
    }

    @DeleteMapping("/{id}/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        events.unregister(currentUser.getUsername(), id);
    }
}
