# Campus Connect with Docker

Docker Compose runs the React frontend, Spring Boot API, and MySQL together. MySQL data and uploaded resources are kept in named volumes.

## Requirements

- Docker Desktop (or Docker Engine) with the Compose plugin
- Ports `8081` (web application) available on the host

## Start locally

From the repository root, create a local environment file from the example:

```powershell
Copy-Item .env.example .env
```

Edit `.env` and replace all three password/secret placeholders with unique, strong values. `JWT_SECRET` must be at least 32 UTF-8 bytes. The `.env` file is ignored by Git; never commit or share it.

Build and start the full application:

```powershell
docker compose up --build -d
docker compose ps
```

Open <http://localhost:8081>. The frontend proxies `/api/` requests to the backend, so the browser and backend share one origin. The backend is not published directly to the host.

Useful commands:

```powershell
docker compose logs -f
docker compose logs -f backend
docker compose restart backend
docker compose down
```

`docker compose down` keeps database and uploaded-file volumes. `docker compose down --volumes` permanently deletes both; use it only when you intentionally want to erase all local application data.

## Deployment notes

- Configure the deployment platform's secret manager with `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, and a unique `JWT_SECRET`; provide `MYSQL_DATABASE`, `APP_PORT`, and `CORS_ALLOWED_ORIGINS` as needed.
- Build and publish the frontend and backend images to a trusted registry, then deploy the Compose services (or translate them to the target orchestrator). Keep MySQL and uploads on persistent storage.
- Terminate HTTPS at a trusted reverse proxy or load balancer and restrict access to the web port as appropriate.
- The application currently uses Hibernate `ddl-auto=update` for its schema. For production upgrades, adopt versioned database migrations and take backups before schema changes.
- Set `CORS_ALLOWED_ORIGINS` to the exact public frontend origin(s), comma-separated and without trailing slashes.

The root `.env` is used for Compose interpolation. The ignored `backend/.env` and `frontend/.env` are for running the modules outside Compose and are not copied into either image.
