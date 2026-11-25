# Licitatii.live: real-time auction platform

[![CI](https://github.com/DannyUreche/auction-engine/actions/workflows/ci.yml/badge.svg)](https://github.com/DannyUreche/auction-engine/actions/workflows/ci.yml)

A full-stack online auction site: bids show up on every screen instantly, auctions close by themselves and
pick the winner, and buyers and sellers get notified by in-app bell, email and browser push.
The interface is in Romanian, with an English toggle.

**Live demo:** https://auction-engine.onrender.com (free hosting, so the first load can take ~1 minute while the server wakes up)

![Home page](docs/screenshots/home.png)

| Item page | Dark mode |
|---|---|
| ![Item page](docs/screenshots/item.png) | ![Dark mode](docs/screenshots/home-dark.png) |

| Wallet | Mobile |
|---|---|
| ![Wallet](docs/screenshots/wallet.png) | <img src="docs/screenshots/mobile.png" alt="Mobile" width="260"> |

## Features

**Auctions**
- Classic (English) auctions with tiered minimum increments, anti-sniping (a bid in the last 2 minutes extends the auction) and optional "Buy now".
- Dutch auctions: the price drops every minute until someone buys.
- Scheduled auctions that open at a chosen time (watchers are notified when they start).
- Proxy bidding: set a maximum and the server bids for you, only as much as needed.
- Virtual wallet: bids are limited to the money you have available; it is settled when the auction closes.

**Live and social**
- Server-Sent Events: prices, the 3D showcase, charts and the bid ticker update without reloading.
- Notifications in the bell, by email (SMTP) and as browser push (Web Push with VAPID), even when the site is closed.
- Watchlist, recommendations ("You might like"), public seller profiles with ratings and reviews.
- Public questions and answers on each item, private chat between seller and winner after the sale.
- Sharing with link previews (Open Graph tags rendered by the server) and a QR code.

**Accounts and safety**
- JWT login, sign-in with Google, email verification, password reset, rate limiting on the auth endpoints.
- AI listing assistant: Claude suggests the name, description, category and starting price from a photo.
- Built-in error monitoring: unexpected server errors and JavaScript errors from browsers are grouped in the admin panel.
- Reporting items, an admin panel with moderation (dismiss / delete), blocking accounts, charts and demo data.

**Interface**
- React 19 + Tailwind CSS v4, animated dot-grid background, CSS 3D carousel, custom neon icon set, dark mode, RO/EN.
- Installable as an app (PWA) with an offline page.
- No chart, animation or 3D libraries: everything is plain SVG, CSS and canvas.

## Architecture

```mermaid
flowchart LR
    Browser["React app<br/>(Vite build)"] -- "REST + JWT" --> API["Spring Boot 4<br/>controllers"]
    API --> Services["Services<br/>bids, wallet, auctions"]
    Services --> DB[("PostgreSQL<br/>Flyway migrations")]
    Scheduler["AuctionScheduler<br/>closes auctions, Dutch prices"] --> Services
    Services -- "events after commit" --> SSE["SSE stream<br/>/api/events"]
    SSE -- "live updates" --> Browser
    Services -- "async" --> Mail["Email (SMTP)"]
    Services -- "async" --> Push["Web Push"]
```

- **Backend:** Java 21, Spring Boot 4 (Web MVC, Security, Data JPA, Mail), PostgreSQL, Flyway, Lombok, springdoc OpenAPI.
- **Frontend:** React 19, React Router, Vite, Tailwind CSS v4 (plain JavaScript).
- **Tests:** about 100 Spring Boot integration tests against a real PostgreSQL, plus Playwright browser tests.
- **CI/CD:** GitHub Actions runs both test suites on every push; Render builds the Docker image and deploys `main`; the database is on Neon.

## Running locally

Requirements: JDK 21+, Node.js 22, Docker.

```bash
docker compose up -d                          # PostgreSQL on port 5433
cp local.properties.example local.properties  # then set jwt.secret (see the file)
./mvnw spring-boot:run                        # API on http://localhost:8080

cd frontend
npm install
npm run dev                                   # UI on http://localhost:5173
```

- API documentation (Swagger UI): http://localhost:8080/swagger-ui.html
- Backend tests: `./mvnw test` (needs the Docker database)
- Browser tests: `cd frontend && npm run test:e2e` (needs the backend running)

Optional environment variables: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` (email
and email verification), `GOOGLE_CLIENT_ID` (sign-in with Google), `ANTHROPIC_API_KEY` (AI listing assistant), `ADMIN_USERNAMES` (admin accounts), `APP_BASE_URL`.
Without them the app still runs: emails are only logged, and the Google button is hidden.

**Emails with Brevo (free, 300 emails/day):** create an account at brevo.com, then in *SMTP & API → SMTP* generate an
SMTP key and verify a sender address. Set `MAIL_HOST=smtp-relay.brevo.com`, `MAIL_PORT=587`, `MAIL_USERNAME` (the SMTP
login shown by Brevo), `MAIL_PASSWORD` (the SMTP key) and `MAIL_FROM` (the verified sender). The admin page then shows
"Email: Configurat" and a *send test email* button.
