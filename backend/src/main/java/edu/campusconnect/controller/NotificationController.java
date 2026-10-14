package edu.campusconnect.controller;

import edu.campusconnect.dto.ApiDtos.NotificationResponse;
import edu.campusconnect.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public Page<NotificationResponse> list(
            @AuthenticationPrincipal UserDetails currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return notifications.list(currentUser.getUsername(), pageable);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(
            @AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        return notifications.markRead(currentUser.getUsername(), id);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal UserDetails currentUser) {
        notifications.markAllRead(currentUser.getUsername());
    }
}
