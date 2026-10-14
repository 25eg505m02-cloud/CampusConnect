package edu.campusconnect.dto;

import edu.campusconnect.entity.Role;
import edu.campusconnect.entity.ReportStatus;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank String password) {
    }

    public record AuthResponse(String token, String tokenType, UserResponse user) {
    }

    public record UserResponse(
            Long id, String fullName, String email, Role role, String bio, String major,
            Integer yearOfStudy, String avatarUrl, Instant createdAt) {
    }

    public record UpdateProfileRequest(
            @Size(max = 100) String fullName,
            @Size(max = 500) String bio,
            @Size(max = 100) String major,
            @Min(1) @Max(10) Integer yearOfStudy,
            @Size(max = 500) String avatarUrl) {
    }

    public record PostRequest(
            @NotBlank @Size(max = 5000) String content,
            Long groupId) {
    }

    public record PostResponse(
            Long id, String content, Long authorId, String authorName, Long groupId,
            String groupName, long likes, long comments, Instant createdAt, Instant updatedAt) {
    }

    public record CommentRequest(@NotBlank @Size(max = 2000) String content) {
    }

    public record CommentResponse(
            Long id, Long postId, Long authorId, String authorName, String content, Instant createdAt) {
    }

    public record GroupRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 2000) String description) {
    }

    public record GroupResponse(
            Long id, String name, String description, Long creatorId, String creatorName,
            long memberCount, Instant createdAt) {
    }

    public record EventRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 4000) String description,
            @NotBlank @Size(max = 300) String venue,
            @NotNull Instant startAt,
            @NotNull Instant endAt,
            @Min(1) Integer capacity) {
    }

    public record EventResponse(
            Long id, String title, String description, String venue, Instant startAt, Instant endAt,
            Integer capacity, long registrations, Long creatorId, String creatorName, Instant createdAt) {
    }

    public record ResourceResponse(
            Long id, String title, String description, String originalFileName, String contentType,
            long fileSize, Long uploaderId, String uploaderName, Instant createdAt) {
    }

    public record ConversationRequest(@NotEmpty @Size(min = 1, max = 49) List<@NotNull Long> participantIds) {
    }

    public record ConversationResponse(
            Long id, List<UserResponse> participants, Instant createdAt, Instant lastMessageAt) {
    }

    public record MessageRequest(@NotBlank @Size(max = 4000) String content) {
    }

    public record MessageResponse(
            Long id, Long conversationId, Long senderId, String senderName, String content, Instant createdAt) {
    }

    public record NotificationResponse(
            Long id, String type, String message, Long referenceId, boolean read, Instant createdAt) {
    }

    public record NoticeRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 5000) String content,
            @Size(max = 100) String category) {
    }

    public record NoticeResponse(
            Long id, String title, String content, String category,
            Long authorId, String authorName, Instant createdAt, Instant updatedAt) {
    }

    public record ReportRequest(
            @NotBlank @Pattern(regexp = "POST|COMMENT|USER|GROUP") String targetType,
            @NotNull @Positive Long targetId,
            @NotBlank @Size(max = 1000) String reason) {
    }

    public record ReportResponse(
            Long id, String targetType, Long targetId, String reason, ReportStatus status,
            Long reporterId, String reporterName, Instant createdAt) {
    }

    public record ReportStatusRequest(@NotNull ReportStatus status) {
    }

    public record RoleRequest(@NotNull Role role) {
    }

    public record AdminDashboardResponse(
            long users, long students, long faculty, long admins, long posts, long groups, long events) {
    }

    public record ApiError(Instant timestamp, int status, String error, String message, String path) {
    }
}
