package edu.campusconnect.service;

import edu.campusconnect.entity.*;
import edu.campusconnect.exception.ApiException;
import edu.campusconnect.repository.*;
import org.springframework.stereotype.Component;

@Component
public class EntityLookup {
    private final UserRepository users;
    private final PostRepository posts;
    private final CampusGroupRepository groups;
    private final EventRepository events;
    private final ConversationRepository conversations;
    private final StudyResourceRepository resources;
    private final CommentRepository comments;
    private final NotificationRepository notifications;
    private final NoticeRepository notices;

    public EntityLookup(UserRepository users, PostRepository posts, CampusGroupRepository groups,
                       EventRepository events, ConversationRepository conversations,
                       StudyResourceRepository resources, CommentRepository comments,
                       NotificationRepository notifications, NoticeRepository notices) {
        this.users = users;
        this.posts = posts;
        this.groups = groups;
        this.events = events;
        this.conversations = conversations;
        this.resources = resources;
        this.comments = comments;
        this.notifications = notifications;
        this.notices = notices;
    }

    public User userByEmail(String email) {
        return users.findByEmailIgnoreCase(email)
                .filter(User::isEnabled)
                .orElseThrow(() -> ApiException.notFound("User not found."));
    }

    public User userById(Long id) {
        return users.findById(id)
                .filter(User::isEnabled)
                .orElseThrow(() -> ApiException.notFound("User not found."));
    }

    public Post post(Long id) {
        return posts.findById(id).orElseThrow(() -> ApiException.notFound("Post not found."));
    }

    public CampusGroup group(Long id) {
        return groups.findById(id).orElseThrow(() -> ApiException.notFound("Group not found."));
    }

    public Event event(Long id) {
        return events.findById(id).orElseThrow(() -> ApiException.notFound("Event not found."));
    }

    public Conversation conversation(Long id) {
        return conversations.findById(id).orElseThrow(() -> ApiException.notFound("Conversation not found."));
    }

    public StudyResource resource(Long id) {
        return resources.findById(id).orElseThrow(() -> ApiException.notFound("Resource not found."));
    }

    public Comment comment(Long id) {
        return comments.findById(id).orElseThrow(() -> ApiException.notFound("Comment not found."));
    }

    public Notification notification(Long id) {
        return notifications.findById(id).orElseThrow(() -> ApiException.notFound("Notification not found."));
    }

    public Notice notice(Long id) {
        return notices.findById(id).orElseThrow(() -> ApiException.notFound("Notice not found."));
    }
}
