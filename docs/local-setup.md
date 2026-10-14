# Local setup

## Prerequisites

- JDK 21
- Maven 3.9 or newer
- MySQL 8 or newer
- Node.js only for the separate Vite frontend

## Configure MySQL and API

Create the development database:

```sql
CREATE DATABASE campus_connect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Edit the ignored `backend/.env` file with the MySQL username/password, a random JWT secret of at least 32 bytes, and the actual frontend origin in `CORS_ALLOWED_ORIGINS`. The Spring Boot config imports this dotenv-style properties file from the backend working directory; environment variables can override it. Never use the sample secret in production.

From PowerShell:

```powershell
cd backend
mvn spring-boot:run
```

The API listens on port 8080 by default. `GET http://localhost:8080/api/v1/health` checks both application and MySQL availability. Maven tests: `mvn test`. Package: `mvn clean package`.

## Frontend

Set `VITE_API_BASE_URL=http://localhost:8080/api/v1` in ignored `frontend/.env`, then run:

```powershell
cd frontend
npm install
npm run dev
```

The frontend starts at `http://localhost:5173`; allow this origin in backend `CORS_ALLOWED_ORIGINS`.

## First administrator

Register a regular account, then promote the designated initial administrator using an authenticated operational process. For local development only, with a direct database connection:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.edu';
```

Do not add a public role parameter to registration. Administrators can assign `FACULTY` after sign-in. All admin role and moderation actions are recorded in `admin_audit`.
