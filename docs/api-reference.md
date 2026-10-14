# API reference

Base URL: `http://localhost:8080/api/v1`. Protected endpoints require `Authorization: Bearer <JWT>`. List endpoints use Spring pagination (`page`, `size`, optional `sort`) and return a page object. Default page size is 20 (messages/comments use 50; admin user/report lists use 50).

## Authentication and users

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/auth/register` | Public | Register a student (`fullName`, `email`, `password`) |
| POST | `/auth/login` | Public | Authenticate (`email`, `password`) and receive JWT |
| GET | `/auth/me` | Authenticated | Current profile |
| POST | `/auth/logout` | Authenticated | Stateless logout acknowledgement; client discards token |
| GET | `/users/{id}` | Authenticated | Public profile |
| PATCH | `/users/me` | Authenticated | Edit profile |
| GET | `/users/search?q=...` | Authenticated | Search active students |
| GET | `/users/me/connections` | Authenticated | Accepted connections |
| GET | `/users/me/connection-requests` | Authenticated | Incoming connection requests |
| POST | `/users/{id}/connections` | Authenticated | Request a connection; reciprocal request accepts |
| PATCH | `/users/me/connections/{requesterId}` | Authenticated | Accept an incoming request |
| DELETE | `/users/me/connections/{userId}` | Authenticated | Remove a connection/request |

## Community

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET, POST | `/posts` | Public read / authenticated write | Feed and create a post (`content`, optional `groupId`); GET supports `q` |
| GET, PATCH, DELETE | `/posts/{id}` | Public read / owner or admin write | View or manage a post |
| POST, DELETE | `/posts/{id}/likes` | Authenticated | Like or unlike (repeat like is idempotent) |
| GET, POST | `/posts/{id}/comments` | Public read / authenticated write | List or add comments |
| DELETE | `/comments/{id}` | Comment author, post author, or admin | Delete comment |
| GET, POST | `/groups` | Public read / authenticated create | Browse or create group (`name`, `description`); creator joins automatically |
| GET | `/groups/{id}` | Public | Group details |
| GET | `/groups/{id}/posts` | Public | Group feed |
| POST | `/groups/{id}/members` | Authenticated | Join group (idempotent) |
| DELETE | `/groups/{id}/members/me` | Authenticated | Leave group |

## Events, notices, and resources

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/events`, `/events/{id}` | Public | Browse events and registration totals |
| POST | `/events` | Faculty or admin | Create event (`title`, `description`, `venue`, `startAt`, `endAt`, optional `capacity`) |
| PATCH, DELETE | `/events/{id}` | Creator or admin | Update/delete event; deletion is blocked if registrations exist |
| POST, DELETE | `/events/{id}/register` | Authenticated | Register/unregister; duplicates and capacity overflow are rejected |
| GET | `/notices`, `/notices/{id}` | Public | Browse notices |
| POST | `/notices` | Faculty or admin | Publish notice; creates user notifications |
| PATCH, DELETE | `/notices/{id}` | Author or admin | Manage notice |
| GET | `/resources`, `/resources/{id}` | Public | Browse/search metadata |
| POST | `/resources` | Authenticated multipart | Upload `title`, optional `description`, `file` (up to 10 MB; PDF, TXT, MD, CSV, DOCX, PPTX, XLSX) |
| GET | `/resources/{id}/download` | Authenticated | Download file |
| DELETE | `/resources/{id}` | Uploader or admin | Delete metadata and stored file |

## Messages, notifications, reports, and administration

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET, POST | `/conversations` | Authenticated | List conversations or create one with `participantIds`; participants must be accepted connections |
| GET, POST | `/conversations/{id}/messages` | Conversation member | Read/send persisted messages |
| GET | `/notifications` | Authenticated | List own notifications |
| PATCH | `/notifications/{id}/read` | Owner | Mark notification read |
| PATCH | `/notifications/read-all` | Authenticated | Mark own notifications read |
| POST | `/reports` | Authenticated | Report a `POST`, `COMMENT`, `USER`, or `GROUP` with `targetId` and `reason` |
| GET | `/admin/dashboard`, `/admin/users`, `/admin/reports` | Admin | Admin statistics and paged moderation lists |
| PATCH | `/admin/users/{id}/role` | Admin | Change role (`role`: `STUDENT`, `FACULTY`, or `ADMIN`) |
| DELETE | `/admin/users/{id}` | Admin | Disable account without destroying associated data |
| PATCH | `/admin/reports/{id}/status` | Admin | Set status to `OPEN`, `REVIEWED`, or `DISMISSED` |
| DELETE | `/admin/posts/{id}` | Admin | Remove reported post |
| GET | `/health` | Public | API and MySQL readiness check |

Validation and domain errors return JSON with `timestamp`, `status`, `error`, `message`, and `path`. Common status codes are 201 for creation, 204 for successful actions without a body, 400 for invalid requests, 401 for missing/invalid authentication, 403 for forbidden actions, 404 for missing records, and 409 for conflicts such as duplicate registrations.
