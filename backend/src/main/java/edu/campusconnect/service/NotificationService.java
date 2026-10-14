package edu.campusconnect.service;

import edu.campusconnect.dto.ApiDtos.NotificationResponse;
import edu.campusconnect.entity.Notification;
import edu.campusconnect.entity.User;
import edu.campusconnect.exception.ApiException;
import edu.campusconnect.repository.NotificationRepository;
import edu.campusconnect.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final EntityLookup lookup;

    public NotificationService(NotificationRepository notifications, UserRepository users, EntityLookup lookup) {
        this.notifications = notifications;
        this.users = users;
        this.lookup = lookup;
    }

    @Transactional
    public void notify(User recipient, String type, String message, Long referenceId) {
        if (recipient != null && recipient.isEnabled()) {
            notifications.save(new Notification(recipient, type, message, referenceId));
        }
    }

    @Transactional
    public void notifyAllActive(String type, String message, Long referenceId) {
        notifications.saveAll(users.findAllByEnabledTrue(org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .map(user -> new Notification(user, type, message, referenceId))
                .toList());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(String email, Pageable pageable) {
        User user = lookup.userByEmail(email);
        return notifications.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable).map(this::toResponse);
    }

    @Transactional
    public NotificationResponse markRead(String email, Long notificationId) {
        User user = lookup.userByEmail(email);
        Notification notification = lookup.notification(notificationId);
        if (!notification.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("You cannot update another user's notification.");
        }
        notification.setRead(true);
        return toResponse(notification);
    }

    @Transactional
    public void markAllRead(String email) {
        User user = lookup.userByEmail(email);
        notifications.markAllReadByUserId(user.getId());
    }

    private NotificationResponse toResponse(Notification item) {
        return new NotificationResponse(item.getId(), item.getType(), item.getMessage(),
                item.getReferenceId(), item.isRead(), item.getCreatedAt());
    }
}
