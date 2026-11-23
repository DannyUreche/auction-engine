# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Spring Boot 4.2 / Java 21 REST API for auctions (PostgreSQL, Flyway, Spring Security + JWT, Lombok, springdoc Swagger).

## Running

- Start the DB first: `docker compose up -d` — Postgres is exposed on **port 5433** (not 5432), db/user/pass `auction_db`/`admin`/`password`.
- Use the Maven wrapper: `./mvnw spring-boot:run`, `./mvnw test` (on Windows PowerShell: `.\mvnw.cmd ...`). `JAVA_HOME` may not be set globally; on the author's machine a JDK is at `C:\Users\urech\.jdks\openjdk-27.0.1.0.3`.
- The JWT secret comes from `local.properties` in the project root (git-ignored, imported via `spring.config.import`) or the `JWT_SECRET` env var; the app won't start without it. See `local.properties.example`.
- `git push` from the agent shell fails ("terminal prompts disabled"): the user's GitHub login lives in VS Code's own auth, not in Git Credential Manager. Commit, then ask the user to push via the VS Code Source Control button.
- Tests are `@SpringBootTest` against the real Postgres — they fail if the docker container isn't running.
- Swagger UI: `http://localhost:8080/swagger-ui.html`. Only `/api/auth/**`, Swagger routes and `/error` are public; everything else needs `Authorization: Bearer <jwt>` from `/api/auth/login`.

## Frontend

- React 19 + Vite + Tailwind CSS v4 + React Router, plain JavaScript (no TypeScript), in `frontend/`.
- Run: backend on 8080 first, then `cd frontend; npm install; npm run dev` → `http://localhost:5173`. Vite proxies `/api` to `localhost:8080`, so the backend needs no CORS config.
- All HTTP calls go through `frontend/src/api.js` (adds the JWT from localStorage, turns `{"eroare": ...}` into thrown `Error`s, fires `auth:expired` on 401). Auth state lives in `frontend/src/auth.jsx`.
- UI text is Romanian without diacritics, same as the backend.

## Database

- Schema is owned by Flyway (`src/main/resources/db/migration/V<n>__*.sql`); Hibernate runs with `ddl-auto=validate`. Any entity change needs a new migration with the next version number — never edit an existing migration.

## Conventions

- Write code comments, user-facing error messages, and commit messages in **Romanian without diacritics**, matching existing code.
- Business-rule violations throw `IllegalArgumentException`; `GlobalExceptionHandler` turns them into HTTP 400 with body `{"eroare": "<message>"}`. Request DTOs use Bean Validation (`@Valid` + `jakarta.validation` annotations with Romanian messages), also returned as 400 `{"eroare": ...}`. Missing/invalid JWT gives 401 in the same format.
- The current user comes from the JWT: take `Authentication authentication` in the controller and pass `authentication.getName()` (username) to the service. Never accept user ids (e.g. `bidderId`, `sellerId`) in request bodies.
- Solo project: committing directly to `main` is fine (only when asked).

## Known gotchas

- `AuctionScheduler` closes expired auctions every 60s and sets `Item.winner` from the highest bid.
- All ids are `Integer` end to end (DB `SERIAL`, entities, repositories, controllers); don't introduce `Long` ids.
- `test-api.http` is written for the VS Code REST Client extension (`# @name` + `{{x.response.body.$.token}}`); keep it in sync when endpoints change.
