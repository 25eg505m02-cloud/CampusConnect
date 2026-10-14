# Database schema

Hibernate creates/updates local tables from the JPA entities. Tables are named `users`, `posts`, `comments`, `post_likes`, `campus_groups`, `group_memberships`, `events`, `event_registrations`, `study_resources`, `conversations`, `conversation_members`, `messages`, `notifications`, `user_connections`, `notices`, `content_reports`, and `admin_audit`.

Core foreign-key relationships:

- A user authors posts, comments, groups, events, study resources, notices, and messages.
- Posts optionally belong to groups; comments and likes reference posts.
- Membership rows link users and groups; event registrations link users and events.
- Conversation memberships link users and conversations; each message references its conversation and sender.
- Notifications, reports, and audit rows retain references to their recipient/reporter/admin.
- Connections retain requester, recipient, and pending/accepted status.

Unique constraints prevent duplicate like, group membership, event registration, and conversation membership pairs. Common list/feed columns have indexes. Responses use DTOs to avoid exposing entity relationships or password hashes and to prevent recursive JSON serialization.

`User.enabled` is used for admin account disablement, preserving history and related data. Physical deletion of an event with registrations is refused; post deletion explicitly removes its comments and likes first. Local uploads use generated storage keys and metadata in `study_resources`.
