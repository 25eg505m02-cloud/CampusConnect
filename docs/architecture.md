# Backend architecture

```text
React / Axios client
        │ HTTPS JSON + Authorization: Bearer JWT
        ▼
Spring Security ── JWT filter ── role and ownership checks
        │
        ▼
REST controllers ── validated DTOs ── domain services
                                       │ transactions
                                       ├── Spring Data JPA ── MySQL
                                       └── controlled local upload storage
```

The backend source is under `backend/src/main/java/edu/campusconnect/`, separated into `controller`, `service`, `repository`, `entity`, `dto`, `security`, `config`, and `exception` packages. Controllers accept and return DTOs; JPA entities are not serialized as API responses. Services own transactions, authorization-sensitive business rules, and entity-to-DTO mapping.

Authentication is stateless. Passwords are BCrypt hashes; access tokens are signed JWTs with subject, user ID, role, and expiry. The JWT filter rechecks the user's active status on each request, so disabled accounts cannot continue using an existing token. Roles are `STUDENT`, `FACULTY`, and `ADMIN`; admin APIs require `ADMIN`, event and notice publishing require faculty or admin, and records with owners enforce ownership in services.

Posts, comments, likes, groups, memberships, events, event registrations, study resources, conversations, conversation memberships, messages, notifications, connections, notices, reports, and admin audit entries persist in MySQL. Unique constraints reinforce duplicate prevention for likes, group membership, event registration, and conversation membership. Message reads and sends verify conversation membership. Resource bytes are stored under a generated UUID filename in `backend/uploads/`; original names are metadata only.

Schema updates use Hibernate `ddl-auto=update` for local development. Use reviewed database migrations and a non-`update` schema policy before production deployment. Configure HTTPS, a strong externally managed JWT secret, database permissions, backups, and durable/private file storage for deployment.
