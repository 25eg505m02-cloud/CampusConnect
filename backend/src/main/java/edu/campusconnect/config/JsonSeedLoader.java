package edu.campusconnect.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.campusconnect.entity.ConnectionStatus;
import edu.campusconnect.entity.ReportStatus;
import edu.campusconnect.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(prefix = "campus-connect.seed", name = "enabled", havingValue = "true")
public class JsonSeedLoader implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(JsonSeedLoader.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final String seedDirectory;
    private int inserted;
    private int skipped;

    public JsonSeedLoader(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            PasswordEncoder passwordEncoder,
            @Value("${campus-connect.seed.directory:../collections}") String seedDirectory) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.seedDirectory = seedDirectory;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        inserted = 0;
        skipped = 0;
        loadUsers();
        loadGroups();
        loadEvents();
        loadConversations();
        loadPosts();
        loadNotices();
        loadStudyResources();
        loadUserConnections();
        loadComments();
        loadPostLikes();
        loadEventRegistrations();
        loadGroupMemberships();
        loadConversationMembers();
        loadMessages();
        loadNotifications();
        loadContentReports();
        loadAdminAudits();
        log.info("Seed import completed: {} inserted, {} already present.", inserted, skipped);
    }

    private void loadUsers() {
        forEach("users.json", row -> insert("users", row,
                "INSERT INTO users (id, full_name, email, password_hash, role, bio, major, year_of_study, "
                        + "avatar_url, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                requiredLong(row, "id"), text(row, "fullName"), text(row, "email"),
                passwordEncoder.encode(text(row, "password")), enumValue(Role.class, row, "role").name(),
                nullableText(row, "bio"), nullableText(row, "major"), nullableInteger(row, "yearOfStudy"),
                nullableText(row, "avatarUrl"), bool(row, "enabled"),
                timestamp(row, "createdAt"), timestamp(row, "updatedAt")));
    }

    private void loadGroups() {
        forEach("campus_groups.json", row -> {
            requireReference("users", row, "creatorId");
            insert("campus_groups", row,
                    "INSERT INTO campus_groups (id, name, description, creator_id, created_at) VALUES (?, ?, ?, ?, ?)",
                    requiredLong(row, "id"), text(row, "name"), text(row, "description"),
                    requiredLong(row, "creatorId"), timestamp(row, "createdAt"));
        });
    }

    private void loadEvents() {
        forEach("events.json", row -> {
            requireReference("users", row, "creatorId");
            insert("events", row,
                    "INSERT INTO events (id, title, description, venue, start_at, end_at, capacity, creator_id, "
                            + "created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    requiredLong(row, "id"), text(row, "title"), text(row, "description"), text(row, "venue"),
                    timestamp(row, "startAt"), timestamp(row, "endAt"), nullableInteger(row, "capacity"),
                    requiredLong(row, "creatorId"), timestamp(row, "createdAt"));
        });
    }

    private void loadConversations() {
        forEach("conversations.json", row -> {
            requireReference("users", row, "createdById");
            insert("conversations", row,
                    "INSERT INTO conversations (id, created_by_id, created_at) VALUES (?, ?, ?)",
                    requiredLong(row, "id"), requiredLong(row, "createdById"), timestamp(row, "createdAt"));
        });
    }

    private void loadPosts() {
        forEach("posts.json", row -> {
            requireReference("users", row, "authorId");
            requireNullableReference("campus_groups", row, "groupId");
            insert("posts", row,
                    "INSERT INTO posts (id, author_id, group_id, content, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    requiredLong(row, "id"), requiredLong(row, "authorId"), nullableLong(row, "groupId"),
                    text(row, "content"), timestamp(row, "createdAt"), timestamp(row, "updatedAt"));
        });
    }

    private void loadNotices() {
        forEach("notices.json", row -> {
            requireReference("users", row, "authorId");
            insert("notices", row,
                    "INSERT INTO notices (id, title, content, category, author_id, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    requiredLong(row, "id"), text(row, "title"), text(row, "content"),
                    nullableText(row, "category"), requiredLong(row, "authorId"),
                    timestamp(row, "createdAt"), timestamp(row, "updatedAt"));
        });
    }

    private void loadStudyResources() {
        forEach("study_resources.json", row -> {
            requireReference("users", row, "uploaderId");
            insert("study_resources", row,
                    "INSERT INTO study_resources (id, title, description, original_file_name, content_type, "
                            + "file_size, storage_key, uploader_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    requiredLong(row, "id"), text(row, "title"), nullableText(row, "description"),
                    text(row, "originalFileName"), text(row, "contentType"), requiredLong(row, "fileSize"),
                    text(row, "storageKey"), requiredLong(row, "uploaderId"), timestamp(row, "createdAt"));
        });
    }

    private void loadUserConnections() {
        forEach("user_connections.json", row -> {
            requireReference("users", row, "requesterId");
            requireReference("users", row, "recipientId");
            insert("user_connections", row,
                    "INSERT INTO user_connections (id, requester_id, recipient_id, status, created_at) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    requiredLong(row, "id"), requiredLong(row, "requesterId"), requiredLong(row, "recipientId"),
                    enumValue(ConnectionStatus.class, row, "status").name(), timestamp(row, "createdAt"));
        });
    }

    private void loadComments() {
        forEach("comments.json", row -> {
            requireReference("posts", row, "postId");
            requireReference("users", row, "authorId");
            insert("comments", row,
                    "INSERT INTO comments (id, post_id, author_id, content, created_at) VALUES (?, ?, ?, ?, ?)",
                    requiredLong(row, "id"), requiredLong(row, "postId"), requiredLong(row, "authorId"),
                    text(row, "content"), timestamp(row, "createdAt"));
        });
    }

    private void loadPostLikes() {
        forEach("post_likes.json", row -> {
            requireReference("posts", row, "postId");
            requireReference("users", row, "userId");
            insert("post_likes", row,
                    "INSERT INTO post_likes (id, post_id, user_id, created_at) VALUES (?, ?, ?, ?)",
                    requiredLong(row, "id"), requiredLong(row, "postId"), requiredLong(row, "userId"),
                    timestamp(row, "createdAt"));
        });
    }

    private void loadEventRegistrations() {
        forEach("event_registrations.json", row -> {
            requireReference("events", row, "eventId");
            requireReference("users", row, "userId");
            insert("event_registrations", row,
                    "INSERT INTO event_registrations (id, event_id, user_id, registered_at) VALUES (?, ?, ?, ?)",
                    requiredLong(row, "id"), requiredLong(row, "eventId"), requiredLong(row, "userId"),
                    timestamp(row, "registeredAt"));
        });
    }

    private void loadGroupMemberships() {
        forEach("group_memberships.json", row -> {
            requireReference("campus_groups", row, "groupId");
            requireReference("users", row, "userId");
            insert("group_memberships", row,
                    "INSERT INTO group_memberships (id, group_id, user_id, joined_at) VALUES (?, ?, ?, ?)",
                    requiredLong(row, "id"), requiredLong(row, "groupId"), requiredLong(row, "userId"),
                    timestamp(row, "joinedAt"));
        });
    }

    private void loadConversationMembers() {
        forEach("conversation_members.json", row -> {
            requireReference("conversations", row, "conversationId");
            requireReference("users", row, "userId");
            insert("conversation_members", row,
                    "INSERT INTO conversation_members (id, conversation_id, user_id, joined_at) "
                            + "VALUES (?, ?, ?, ?)",
                    requiredLong(row, "id"), requiredLong(row, "conversationId"), requiredLong(row, "userId"),
                    timestamp(row, "joinedAt"));
        });
    }

    private void loadMessages() {
        forEach("messages.json", row -> {
            requireReference("conversations", row, "conversationId");
            requireReference("users", row, "senderId");
            insert("messages", row,
                    "INSERT INTO messages (id, conversation_id, sender_id, content, created_at) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    requiredLong(row, "id"), requiredLong(row, "conversationId"), requiredLong(row, "senderId"),
                    text(row, "content"), timestamp(row, "createdAt"));
        });
    }

    private void loadNotifications() {
        forEach("notifications.json", row -> {
            requireReference("users", row, "userId");
            insert("notifications", row,
                    "INSERT INTO notifications (id, user_id, type, message, reference_id, is_read, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    requiredLong(row, "id"), requiredLong(row, "userId"), text(row, "type"),
                    text(row, "message"), nullableLong(row, "referenceId"), bool(row, "read"),
                    timestamp(row, "createdAt"));
        });
    }

    private void loadContentReports() {
        forEach("content_reports.json", row -> {
            requireReference("users", row, "reporterId");
            insert("content_reports", row,
                    "INSERT INTO content_reports (id, target_type, target_id, reason, status, reporter_id, "
                            + "created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    requiredLong(row, "id"), text(row, "targetType"), requiredLong(row, "targetId"),
                    text(row, "reason"), enumValue(ReportStatus.class, row, "status").name(),
                    requiredLong(row, "reporterId"),
                    timestamp(row, "createdAt"));
        });
    }

    private void loadAdminAudits() {
        forEach("admin_audit.json", row -> {
            requireReference("users", row, "adminId");
            insert("admin_audit", row,
                    "INSERT INTO admin_audit (id, admin_id, action, target_type, target_id, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    requiredLong(row, "id"), requiredLong(row, "adminId"), text(row, "action"),
                    text(row, "targetType"), requiredLong(row, "targetId"), timestamp(row, "createdAt"));
        });
    }

    private void insert(String table, JsonNode row, String sql, Object... values) {
        long id = requiredLong(row, "id");
        if (exists(table, id)) {
            skipped++;
            return;
        }
        if ("users".equals(table) && jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?)", Integer.class, text(row, "email")) > 0) {
            throw invalid(row, "email", "already belongs to a user with a different ID");
        }
        jdbcTemplate.update(sql, values);
        inserted++;
    }

    private void requireReference(String table, JsonNode row, String field) {
        long id = requiredLong(row, field);
        if (!exists(table, id)) {
            throw invalid(row, field, "references missing " + table + " ID " + id);
        }
    }

    private void requireNullableReference(String table, JsonNode row, String field) {
        if (row.path(field).isMissingNode() || row.path(field).isNull()) {
            return;
        }
        requireReference(table, row, field);
    }

    private boolean exists(String table, long id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id) > 0;
    }

    private void forEach(String fileName, Consumer<JsonNode> action) {
        JsonNode records;
        try (InputStream input = open(fileName)) {
            records = objectMapper.readTree(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read seed collection " + fileName
                    + " from " + seedDirectory, exception);
        }
        if (records == null || !records.isArray()) {
            throw new IllegalStateException("Seed collection " + fileName + " must contain a JSON array.");
        }
        int index = 0;
        for (JsonNode row : records) {
            if (!row.isObject()) {
                throw new IllegalStateException("Seed collection " + fileName
                        + " record " + index + " must be a JSON object.");
            }
            try {
                action.accept(row);
            } catch (RuntimeException exception) {
                throw new IllegalStateException("Invalid seed data in " + fileName
                        + " record " + index + ": " + exception.getMessage(), exception);
            }
            index++;
        }
    }

    private InputStream open(String fileName) throws IOException {
        Path directory = Path.of(seedDirectory).toAbsolutePath().normalize();
        Path file = directory.resolve(fileName).normalize();
        if (!file.startsWith(directory)) {
            throw new IllegalStateException("Seed collection path escapes the configured directory.");
        }
        return Files.newInputStream(file);
    }

    private static String text(JsonNode row, String field) {
        JsonNode value = row.get(field);
        if (value == null || value.isNull() || !value.isValueNode()) {
            throw invalid(row, field, "must be a non-null scalar value");
        }
        String result = value.asText();
        if (result.isBlank()) {
            throw invalid(row, field, "must not be blank");
        }
        return result;
    }

    private static String nullableText(JsonNode row, String field) {
        JsonNode value = row.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static long requiredLong(JsonNode row, String field) {
        JsonNode value = row.get(field);
        if (value == null || !value.canConvertToLong()) {
            throw invalid(row, field, "must be an integer");
        }
        return value.longValue();
    }

    private static Long nullableLong(JsonNode row, String field) {
        JsonNode value = row.get(field);
        return value == null || value.isNull() ? null : requiredLong(row, field);
    }

    private static Integer nullableInteger(JsonNode row, String field) {
        JsonNode value = row.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.canConvertToInt()) {
            throw invalid(row, field, "must be an integer");
        }
        return value.intValue();
    }

    private static boolean bool(JsonNode row, String field) {
        JsonNode value = row.get(field);
        if (value == null || !value.isBoolean()) {
            throw invalid(row, field, "must be true or false");
        }
        return value.booleanValue();
    }

    private static Timestamp timestamp(JsonNode row, String field) {
        try {
            return Timestamp.from(Instant.parse(text(row, field)));
        } catch (RuntimeException exception) {
            throw invalid(row, field, "must be an ISO-8601 instant");
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> enumType, JsonNode row, String field) {
        try {
            return Enum.valueOf(enumType, text(row, field));
        } catch (RuntimeException exception) {
            throw invalid(row, field, "is not a valid " + enumType.getSimpleName());
        }
    }

    private static IllegalArgumentException invalid(JsonNode row, String field, String message) {
        return new IllegalArgumentException("record ID " + row.path("id").asText("?")
                + " field '" + field + "' " + message + ".");
    }
}
