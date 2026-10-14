package edu.campusconnect.service;

import edu.campusconnect.dto.ApiDtos.UpdateProfileRequest;
import edu.campusconnect.dto.ApiDtos.UserResponse;
import edu.campusconnect.entity.ConnectionStatus;
import edu.campusconnect.entity.User;
import edu.campusconnect.entity.UserConnection;
import edu.campusconnect.exception.ApiException;
import edu.campusconnect.repository.UserConnectionRepository;
import edu.campusconnect.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository users;
    private final UserConnectionRepository connections;
    private final EntityLookup lookup;
    private final NotificationService notifications;

    public UserService(UserRepository users, UserConnectionRepository connections,
                       EntityLookup lookup, NotificationService notifications) {
        this.users = users;
        this.connections = connections;
        this.lookup = lookup;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return toResponse(lookup.userById(id));
    }

    @Transactional
    public UserResponse update(String email, UpdateProfileRequest request) {
        User user = lookup.userByEmail(email);
        if (request.fullName() != null) {
            if (request.fullName().isBlank()) {
                throw ApiException.badRequest("Full name must not be blank.");
            }
            user.setFullName(request.fullName().trim());
        }
        if (request.bio() != null) {
            user.setBio(request.bio().trim());
        }
        if (request.major() != null) {
            user.setMajor(request.major().trim());
        }
        if (request.yearOfStudy() != null) {
            user.setYearOfStudy(request.yearOfStudy());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().trim());
        }
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> search(String query, Pageable pageable) {
        String term = query == null ? "" : query.trim();
        if (term.isEmpty()) {
            throw ApiException.badRequest("Search query 'q' must not be empty.");
        }
        return users.findByEnabledTrueAndFullNameContainingIgnoreCaseOrEnabledTrueAndEmailContainingIgnoreCase(
                term, term, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> connections(String email) {
        User current = lookup.userByEmail(email);
        return connections.findByRequesterIdAndStatusOrRecipientIdAndStatus(
                        current.getId(), ConnectionStatus.ACCEPTED, current.getId(), ConnectionStatus.ACCEPTED)
                .stream()
                .map(connection -> toResponse(connection.getRequester().getId().equals(current.getId())
                        ? connection.getRecipient() : connection.getRequester()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> incomingConnectionRequests(String email) {
        User current = lookup.userByEmail(email);
        return connections.findByRecipientIdAndStatus(current.getId(), ConnectionStatus.PENDING)
                .stream()
                .map(connection -> toResponse(connection.getRequester()))
                .toList();
    }

    @Transactional
    public void requestConnection(String email, Long targetId) {
        User requester = lookup.userByEmail(email);
        User recipient = lookup.userById(targetId);
        if (requester.getId().equals(recipient.getId())) {
            throw ApiException.badRequest("You cannot connect with yourself.");
        }
        UserConnection existing = connections.findByRequesterIdAndRecipientId(requester.getId(), recipient.getId())
                .or(() -> connections.findByRequesterIdAndRecipientId(recipient.getId(), requester.getId()))
                .orElse(null);
        if (existing != null) {
            if (existing.getStatus() == ConnectionStatus.ACCEPTED) {
                return;
            }
            if (existing.getRequester().getId().equals(recipient.getId())) {
                existing.setStatus(ConnectionStatus.ACCEPTED);
                notifications.notify(recipient, "CONNECTION_ACCEPTED",
                        requester.getFullName() + " accepted your connection request.", requester.getId());
                return;
            }
            throw ApiException.conflict("A connection request is already pending.");
        }
        UserConnection connection = new UserConnection();
        connection.setRequester(requester);
        connection.setRecipient(recipient);
        connections.save(connection);
        notifications.notify(recipient, "CONNECTION_REQUEST",
                requester.getFullName() + " sent you a connection request.", requester.getId());
    }

    @Transactional
    public void removeConnection(String email, Long targetId) {
        User current = lookup.userByEmail(email);
        User target = lookup.userById(targetId);
        UserConnection relation = connections.findByRequesterIdAndRecipientId(current.getId(), target.getId())
                .or(() -> connections.findByRequesterIdAndRecipientId(target.getId(), current.getId()))
                .orElseThrow(() -> ApiException.notFound("Connection request not found."));
        connections.delete(relation);
    }

    @Transactional
    public void acceptConnection(String email, Long requesterId) {
        User recipient = lookup.userByEmail(email);
        UserConnection request = connections.findByRequesterIdAndRecipientId(requesterId, recipient.getId())
                .orElseThrow(() -> ApiException.notFound("Connection request not found."));
        if (request.getStatus() == ConnectionStatus.ACCEPTED) {
            return;
        }
        request.setStatus(ConnectionStatus.ACCEPTED);
        notifications.notify(request.getRequester(), "CONNECTION_ACCEPTED",
                recipient.getFullName() + " accepted your connection request.", recipient.getId());
    }

    User findActiveUser(String email) {
        return lookup.userByEmail(email);
    }

    UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getBio(), user.getMajor(), user.getYearOfStudy(), user.getAvatarUrl(), user.getCreatedAt());
    }
}
