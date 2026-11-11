# QA SmartPredict AI — Docker Compose patch

Copy these files into the corresponding paths at the root of your existing qa-smartpredict-ai project. Do not delete your source files.

1. Copy `.env.example` to `.env` and set passwords and your optional OpenRouter API key.
2. From the project root, run `docker compose config` then `docker compose up -d --build`.
3. Frontend: http://localhost:4200 ; API: http://localhost:8081 ; Keycloak: http://localhost:8082 ; PostgreSQL: localhost:5432.

Notes:
- Existing Angular services use `http://localhost:8081`; this works from a browser on the same PC. For remote access, change them to a configurable base URL or reverse proxy.
- Existing Keycloak frontend config uses `localhost:8082` and redirects to `localhost:4200`; preserve the realm `qa-smartpredict` and client `qa-smartpredict-ui`, including valid redirect URIs and web origins.
- The original Keycloak service uses ephemeral dev storage. This patch intentionally does not change its storage to avoid claiming it preserves an existing realm. Export/backup the realm and users before changing persistence.
- PostgreSQL named volume is pinned to `qa-smartpredict_postgres_data` but verify your existing volume with `docker volume ls` before any changes. Never run `docker compose down -v` on valuable data.
- The project has both `application.yml` and `application.properties`; verify runtime configuration precedence, especially the OpenRouter key, and test AI endpoints.
- This is a prepared configuration, not a validated running deployment. Maven build, Angular build, database migrations and CORS must be tested on your machine.
