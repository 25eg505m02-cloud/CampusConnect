# Backend testing checklist

Run the automated backend suite from `backend/` with `mvn test`. The current integration suite runs against an in-memory H2 database in MySQL compatibility mode. The clean verification command `mvn clean verify` completed successfully: 3 tests passed, 0 failed, 0 errors. Also run the checklist below against MySQL before deployment.

## End-to-end checks

- [ ] Database readiness returns 200 with MySQL and 503 when the database is unavailable.
- [ ] Register a student; confirm BCrypt hash persistence and no password/hash in response.
- [ ] Login with valid credentials; `/auth/me` accepts the bearer token.
- [ ] Invalid login and missing/expired tokens return 401; student calls to admin endpoints return 403.
- [ ] Profile updates cannot change a user's role or email.
- [ ] Post CRUD, group posting membership rules, likes, comments, and deletion ownership.
- [ ] Group creator membership, join/leave, and duplicate membership prevention.
- [ ] Faculty/admin event and notice management; student publication is rejected.
- [ ] Event date validation, duplicate registration rejection, and capacity enforcement.
- [ ] Upload permitted file signatures/types and reject oversize, invalid, and unsupported files.
- [ ] Only upload owner/admin can delete resources; downloads require authentication.
- [ ] Only accepted connections can start conversations; non-members cannot read or send messages.
- [ ] Users can only read/mark their own notifications; reports and admin moderation are audited.
- [ ] Admin cannot disable/demote itself or remove the last active admin.
- [ ] CORS permits configured frontend origin and rejects unconfigured origins.
