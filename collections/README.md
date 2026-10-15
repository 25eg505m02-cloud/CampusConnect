# Campus Connect sample seed collections

These JSON files contain sample data for all 17 persisted backend entity types. They are intended for development or test databases, not production.

Enable the Spring Boot JSON seed loader by setting these values in `backend/.env`:

```properties
CAMPUS_CONNECT_SEED_ENABLED=true
CAMPUS_CONNECT_SEED_DIRECTORY=../collections
```

The loader inserts collections in foreign-key order at application startup. It skips records whose IDs already exist; it does not update existing records. Missing, malformed, or inconsistent collection files fail startup and report the affected file and record. Disable seeding after the initial load by setting `CAMPUS_CONNECT_SEED_ENABLED=false`.

Each row has an explicit numeric `id`; related rows refer to those IDs using fields such as `authorId`, `creatorId`, and `postId`. Seed an empty development database where possible. Existing IDs are preserved, but IDs in these fixtures may overlap unrelated records in an existing database.

`users.json` uses a seed-only `password` field set to `12345678`. The loader BCrypt-encodes each password before storing it as `passwordHash`. Do not use these shared credentials in production.

`study_resources.json` contains metadata only. The referenced files are not included, so downloads will not work until matching files are uploaded and their `storageKey` values are updated.
