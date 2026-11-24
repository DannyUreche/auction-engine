# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Spring Boot 4.2 / Java 21 REST API for auctions (PostgreSQL, Flyway, Spring Security + JWT, Lombok, springdoc Swagger).

## Running

- Start the DB first: `docker compose up -d` — Postgres is exposed on **port 5433** (not 5432), db/user/pass `auction_db`/`admin`/`password`.
- Use the Maven wrapper: `./mvnw spring-boot:run`, `./mvnw test` (on Windows PowerShell: `.\mvnw.cmd ...`). `JAVA_HOME` may not be set globally; on the author's machine a JDK is at `C:\Users\urech\.jdks\openjdk-27.0.1.0.3`.
- The JWT secret comes from `local.properties` in the project root (git-ignored, imported via `spring.config.import`) or the `JWT_SECRET` env var; the app won't start without it. See `local.properties.example`.
- `git push` from the agent shell fails ("terminal prompts disabled"): the user's GitHub login lives in VS Code's own auth, not in Git Credential Manager. Commit, then ask the user to push via the VS Code Source Control button.
- Tests are `@SpringBootTest` against the real Postgres — they fail if the docker container isn't running.
- Swagger UI: `http://localhost:8080/swagger-ui.html`. Public (no token): `/api/auth/**`, Swagger, `/error`, `GET /api/images/**`, `GET /api/events`, and read-only `GET /api/items`, `/api/items/**`, `/api/bids/**` — except `/api/items/my`, `/api/items/won`, `/api/bids/my`, which are declared authenticated *before* the public GET matchers in `SecurityConfig` (order matters). Everything else needs `Authorization: Bearer <jwt>` from `/api/auth/login`. In the frontend, `/` and `/items/:id` are public; `/sell` and `/account` are wrapped in `RequireAuth`.

## Frontend

- React 19 + Vite + Tailwind CSS v4 + React Router, plain JavaScript (no TypeScript), in `frontend/`.
- Run: backend on 8080 first, then `cd frontend; npm install; npm run dev` → `http://localhost:5173`. Vite proxies `/api` to `localhost:8080`, so the backend needs no CORS config.
- All HTTP calls go through `frontend/src/api.js` (adds the JWT from localStorage, turns `{"eroare": ...}` into thrown `Error`s, fires `auth:expired` on 401). Auth state lives in `frontend/src/auth.jsx`.
- UI text is Romanian without diacritics, same as the backend.
- Visual style: animated backdrop (`components/Backdrop.jsx`), custom animations/utilities (`animate-fade-up`, `animate-price-flash`, `text-gradient`, `bg-grid`, `skeleton`, `spotlight`) defined in `src/index.css` with `@theme`/`@utility` (Tailwind v4, no config file) and disabled under `prefers-reduced-motion`. Home hero uses public `GET /api/stats` and `GET /api/bids/recent` (last 8 bids) for live counters (`CountUp`) and the `ActivityFeed`; item page has an SVG step `PriceChart` and `launchConfetti()` (`src/confetti.js`) when you win. No chart/animation libraries - keep it dependency-free.

## Deployment (Render)

- `render.yaml` (Blueprint) defines only the Docker web service `auction-engine`, region Frankfurt. The production DB is on **Neon** (free, doesn't expire; Frankfurt), not Render: env vars `DATABASE_URL` (JDBC form, `jdbc:postgresql://<host>/neondb?sslmode=require`, use the *direct* host, not `-pooler`, because of Flyway), `DATABASE_USERNAME`, `DATABASE_PASSWORD` are `sync: false` and set in the Render dashboard. `JWT_SECRET` is generated; `PORT` is set by Render. Locally all of these fall back to the defaults in `application.properties` (Docker Postgres on 5433).
- `Dockerfile` builds the React app, copies `frontend/dist` into `src/main/resources/static`, and packages one Spring Boot jar (tests skipped in Docker — they need Postgres). In production there is no Vite: Spring serves the UI; `SpaController` forwards React routes (`/items/{id}`, `/login`, ...) to `index.html`. When adding a new React route, add it there too.
- Render deploys automatically on every push to `main`. Live URL: https://auction-engine.onrender.com (first deployed 2026-11-23). The production DB is separate from the local one and started empty.

## Database

- Schema is owned by Flyway (`src/main/resources/db/migration/V<n>__*.sql`); Hibernate runs with `ddl-auto=validate`. Any entity change needs a new migration with the next version number — never edit an existing migration.

## Conventions

- Write code comments, user-facing error messages, and commit messages in **Romanian without diacritics**, matching existing code.
- Business-rule violations throw `IllegalArgumentException`; `GlobalExceptionHandler` turns them into HTTP 400 with body `{"eroare": "<message>"}`. Request DTOs use Bean Validation (`@Valid` + `jakarta.validation` annotations with Romanian messages), also returned as 400 `{"eroare": ...}`. Missing/invalid JWT gives 401 in the same format.
- The current user comes from the JWT: take `Authentication authentication` in the controller and pass `authentication.getName()` (username) to the service. Never accept user ids (e.g. `bidderId`, `sellerId`) in request bodies.
- Solo project: committing directly to `main` is fine (only when asked).

## Known gotchas

- `AuctionScheduler` closes expired auctions every 60s and sets `Item.winner` from the highest bid.
- Item photos: up to `Item.MAX_IMAGES` (5) per item. Bytes live in `item_images` (bytea, V9) because Render's free plan has no persistent disk; the ordered list is `item_photos` (V12: `item_id`, `position`, `image_filename`), mapped as an `@ElementCollection` `Item.imageFilenames` with `@OrderColumn` (position 0 = main photo shown on cards) and `@Fetch(SUBSELECT)`. JSON exposes `imageUrls` (all) and `imageUrl` (main). Seller-only endpoints: `POST /api/items/{id}/images` (multipart field `image`, max 5 MB, type detected from magic bytes in `ImageStorageService`), `DELETE /api/items/{id}/images/{filename}`, `POST /api/items/{id}/images/{filename}/main`. Delete a photo from the list (and flush) *before* deleting its `item_images` row (FK). `GET /api/images/**` is public because `<img>` can't send the JWT.
- Watchlist ("Urmaresc"): `watched_items` (V13, unique per user+item, `ON DELETE CASCADE` on the item). `GET /api/items/watched` (declared authenticated before the public GET matchers), `POST`/`DELETE /api/items/{id}/watch` (idempotent; can't watch your own item). Frontend: `WatchlistProvider`/`useWatchlist`/`WatchButton` in `frontend/src/watchlist.jsx` (wraps `App` in `main.jsx`); it shows toast notifications for live `bid`/`closed` events on watched items (skipped on that item's own page and for your own bids).
- Live updates: `BidService` and `AuctionScheduler` publish an `AuctionEvent` (`bid` / `closed`); `AuctionEventService` forwards it via `@TransactionalEventListener(AFTER_COMMIT)` to all `SseEmitter`s from `GET /api/events` (public, Server-Sent Events, 25s heartbeat). Frontend subscribes with `useAuctionEvents` in `frontend/src/events.jsx`, which shares ONE `EventSource` per tab between all subscribers (browsers cap open HTTP/1.1 connections per host); polling (30s) remains only as a fallback.
- User objects nested in items/bids (`seller`, `winner`, `bidder`) hide `email` via `@JsonIgnoreProperties`. Emails are exposed only by `/api/users/me` and by `GET /api/items/{id}/contact` (authenticated, declared before the public GET matchers): after a sale, the seller gets the winner's email and vice versa, nobody else. Public seller profile: `GET /api/users/{username}/profile` (permitAll, no email; active + sold items), React route `/users/:username` (also in `SpaController`).
- Categories: fixed `Category` enum (stored as string, V11, default `ALTELE`), listed publicly by `GET /api/categories`; frontend loads them once via `useCategories()` in `frontend/src/categories.js`. Filtering/sorting on the home page is client-side over `/api/items/active`; filter state lives in local React state and is mirrored to the URL (`?cat=&q=&min=&max=&sort=`) — don't drive inputs directly from `useSearchParams` (React Router 8 updates the URL in a transition and rapid updates get lost).
- Editing: `PUT /api/items/{id}` (seller only, only while active and with zero bids; same fields as create minus duration, `currentPrice` reset to `startPrice`, `endTime` unchanged). Frontend: inline `EditItemForm` on the item page.
- Bidding rules live in `BidRules`: tiered minimum increment (<100: 1, <1000: 5, <10000: 10, else 50 RON; exposed as `Item.minimumBid`), anti-sniping (a bid with <2 min left moves `endTime` to now + 2 min), optional `buyNowPrice` (V10) — `POST /api/items/{id}/buy-now` records a bid at that price and closes the auction via `AuctionScheduler.closeAuction`; available only while `currentPrice < buyNowPrice` (`Item.buyNowAvailable`).
- Admins: usernames listed in `app.admin-usernames` (env `ADMIN_USERNAMES`, comma-separated; locally via `local.properties`) get `ROLE_ADMIN` in `CustomUserDetailsService` (via `AdminAccess`). `/api/admin/**` is `hasRole("ADMIN")` (403 JSON otherwise); `/api/users/me` returns `admin: true/false` and the frontend shows `/admin` only then. Admin can close an auction now (reuses `AuctionScheduler.closeAuction`) and delete items (bids deleted first via `BidRepository.deleteAllForItem`, then the item, then its photos). No user deletion on purpose.
- Time zone: `main()` forces the JVM default to UTC, so every `LocalDateTime` (endTime, createdAt) is UTC both locally and on Render. JSON dates have no zone suffix; the frontend must read them with `parseDate()` from `frontend/src/api.js` (appends `Z`), never `new Date(value)` directly. (Tests don't go through `main()`, which is fine since they're self-consistent.)
- All ids are `Integer` end to end (DB `SERIAL`, entities, repositories, controllers); don't introduce `Long` ids.
- `test-api.http` is written for the VS Code REST Client extension (`# @name` + `{{x.response.body.$.token}}`); keep it in sync when endpoints change.
