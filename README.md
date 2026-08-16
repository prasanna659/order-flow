# OrderFlow

A production-grade event-driven order processing platform built to demonstrate distributed systems patterns in action. Every order placed triggers a real Saga — reserves stock, charges payment, publishes a Kafka event, and sends a transactional email — with automatic compensation when anything fails.

**Stack:** Spring Boot 3.2 · Java 17 · Spring Cloud 2023 · Apache Kafka (KRaft) · React 18 · Tailwind CSS · Docker Compose

---

## Architecture

```
Browser (React :3000)
       │
       ▼
API Gateway :8080  ── JWT validation · routing · CORS
       │
       ├──▶ Auth Service        :8081  ── MySQL  (authdb)
       ├──▶ Inventory Service   :8082  ── PostgreSQL (inventorydb)
       ├──▶ Order Service       :8083  ── MySQL  (orderdb)  ──▶ Kafka
       ├──▶ Payment Service     :8084  ── in-memory (~15% failure sim)
       └──▶ Notification Svc    :8085  ── Kafka consumer ──▶ Gmail SMTP

Platform: Eureka :8761 · Config Server :8888 · Kafka :9092
```

### Services at a glance

| Service | Port | Role | Persistence |
|---|---|---|---|
| API Gateway | 8080 | Routing, JWT filter, CORS | — |
| Auth Service | 8081 | Register / login / Google OAuth2 / password reset | MySQL |
| Inventory Service | 8082 | Product catalog, stock reserve & release | PostgreSQL |
| Order Service | 8083 | Saga orchestrator, order lifecycle | MySQL |
| Payment Service | 8084 | Charge simulation (~15% random decline) | In-memory |
| Notification Service | 8085 | Kafka consumer, transactional email via Gmail SMTP | — |
| Eureka Server | 8761 | Service discovery registry | — |
| Config Server | 8888 | Centralised config (serves `/config-repo/*.yml`) | — |

---

## Distributed Systems Patterns

### Saga (Orchestration)

Orders run a 3-step saga on a background async thread. The HTTP response returns immediately with `PENDING` — the frontend polls every second to animate status transitions.

```
PENDING → RESERVING → CHARGING → CONFIRMED
                               ↘ CANCELLED  (compensation executed)
```

**Happy path**
1. Order Service creates order (`PENDING`)
2. Inventory Service reserves stock (`RESERVING`)
3. Payment Service charges (`CHARGING`)
4. Order saved as `CONFIRMED`, `order-events` Kafka message published
5. Notification Service consumes the event → sends confirmation email

**Compensation path (payment fails)**
1. Steps 1–2 succeed
2. Payment declined → `releaseStock()` called on Inventory Service
3. Order saved as `CANCELLED`, cancellation event published
4. Notification Service sends cancellation email with failure reason

Any unexpected exception (inventory timeout, circuit breaker open) also triggers the compensation path and publishes the cancellation event — no silent failures.

### Circuit Breakers (Resilience4j)

Wraps every downstream call inside the Order Service saga:

- Sliding window: 10 calls
- Failure rate threshold: 50%
- Open → wait 10s → half-open with 3 probe calls
- When open: saga fails fast, compensates, cancels order

### Event-Driven Notifications

Two Kafka topics are pre-declared at startup (`KafkaTopicConfig`) so consumers never race against first publish:

- `order-events` — CONFIRMED and CANCELLED order lifecycle events
- `auth-events` — PASSWORD_RESET events

Each email type is rendered with a Thymeleaf HTML template and sent with `@Retryable` (4 attempts, 5s → 10s → 20s backoff). SMTP failures are retried silently without affecting order state.

### JWT Authentication

JWTs are issued by Auth Service and validated at the API Gateway (`AuthGatewayFilterFactory`). Validated user identity is forwarded downstream as `X-User-Id` / `X-Username` headers — no downstream service re-parses the token.

Google OAuth2 bypasses the gateway entirely (direct to Auth Service :8081) because WebFlux cannot proxy stateful servlet OAuth2 redirects.

### Database Per Service

- Auth + Order → MySQL 8 (separate schemas: `authdb`, `orderdb`)
- Inventory → PostgreSQL 16 (`inventorydb`)
- Payment → in-memory map (demo only)
- No shared database, no shared schema coupling

---

## Product Catalog

20 real tech products across 6 categories, seeded fresh on every startup:

| Category | Products |
|---|---|
| Audio | Sony WH-1000XM5, Bose QuietComfort Ultra Earbuds, Marshall Emberton II |
| Computing | Keychron Q3 Pro, Logitech MX Keys S, Samsung T7 SSD, Twelve South HiRise, CalDigit TS4 |
| Wearables | Apple Watch Ultra 2, Garmin Forerunner 965, Oura Ring Gen 3 |
| Cameras | Sony ZV-E10 II, DJI Osmo Pocket 3, Elgato Facecam Pro |
| Power | Ugreen Nexode 160W, Anker SOLIX C200, Belkin BoostCharge Pro 3-in-1 |
| Displays | LG 32GQ850-B Monitor (sold out), BenQ ScreenBar Halo, Elgato Stream Deck + |

The catalog UI supports:
- Category filter pills with live counts and per-category colour accents
- Real-time search by name or description
- Animated card entrance (staggered Framer Motion)
- "Added ✓" button feedback, low-stock badge, sold-out overlay

---

## Getting Started

### Prerequisites

- Docker Desktop (or Docker Engine + Compose plugin)
- A Gmail account with a [Gmail App Password](https://myaccount.google.com/apppasswords) (2FA required)
- A Google OAuth2 client from [Google Cloud Console](https://console.cloud.google.com/) *(optional — email/password auth works without it)*

### 1. Configure `.env`

The `.env` file is at `orderflow/.env`. Fill in your values:

```env
# Google OAuth2 (optional)
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-...

# Frontend URL — used for OAuth2 redirects and email links
FRONTEND_URL=http://localhost:3000

# Email — requires a Gmail App Password (not your real Gmail password)
MAIL_ENABLED=true
MAIL_USERNAME=you@gmail.com
MAIL_PASSWORD=xxxx xxxx xxxx xxxx
```

To disable emails, set `MAIL_ENABLED=false`. Everything still works — email sends are skipped and logged.

### 2. Google OAuth2 redirect URI (if using Google login)

In Google Cloud Console → Credentials → your OAuth2 client, add to **Authorized redirect URIs**:

```
http://localhost:8081/login/oauth2/code/google
```

### 3. Start everything

```bash
cd orderflow
docker compose up --build
```

First build takes 3–5 minutes (Maven downloads dependencies). Services start in healthcheck dependency order:

```
infrastructure (MySQL, Postgres, Kafka)
  → config-server
    → eureka-server
      → business services (auth, inventory, payment, order, notification)
        → api-gateway
          → frontend
```

Open **http://localhost:3000** once the frontend container is healthy.

### 4. Explore

| URL | What it is |
|---|---|
| http://localhost:3000 | React app — landing, catalog, cart, orders |
| http://localhost:3000/architecture | Interactive architecture diagram |
| http://localhost:3000/health | Live health dashboard (all services + infra) |
| http://localhost:8761 | Eureka service registry |
| http://localhost:8080/docs/auth/swagger-ui.html | Auth Service Swagger UI |
| http://localhost:8080/docs/orders/swagger-ui.html | Order Service Swagger UI |
| http://localhost:8080/docs/inventory/swagger-ui.html | Inventory Service Swagger UI |
| http://localhost:8080/docs/payment/swagger-ui.html | Payment Service Swagger UI |
| http://localhost:8080/docs/notification/swagger-ui.html | Notification Service Swagger UI |

---

## Project Structure

```
orderflow/
├── api-gateway/            Spring Cloud Gateway — routing & JWT filter
├── auth-service/           Register, login, Google OAuth2, password reset
├── inventory-service/      Product catalog, stock reserve/release
├── order-service/          Saga orchestrator, order CRUD
├── payment-service/        Charge simulation (15% random failure)
├── notification-service/   Kafka consumer, Thymeleaf email templates
├── eureka-server/          Netflix Eureka service registry
├── config-server/          Spring Cloud Config Server
├── config-repo/            YAML configs served to all services at startup
│   ├── application.yml         Shared: Eureka, JWT, Actuator
│   ├── auth-service.yml        MySQL, Kafka producer, Google OAuth2
│   ├── order-service.yml       MySQL, Kafka producer, Resilience4j
│   ├── inventory-service.yml   PostgreSQL
│   └── notification-service.yml Kafka consumer, Gmail SMTP, frontend URL
├── frontend/               React 18 + Vite + Tailwind SPA
├── docker-compose.yml      Full stack orchestration
└── .env                    Runtime secrets (never commit real credentials)
```

---

## API Reference

All routes go through the API Gateway on `:8080`. JWT required unless marked public.

### Auth — `/api/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register with username, email, password |
| POST | `/api/auth/login` | Public | Login, returns JWT |
| POST | `/api/auth/forgot-password` | Public | Send password reset email (always returns 200) |
| GET | `/api/auth/reset-password/validate` | Public | Validate reset token (non-consuming) |
| POST | `/api/auth/reset-password` | Public | Set new password using token |
| GET | `/login/oauth2/code/google` | Public | Google OAuth2 callback (direct :8081) |

### Inventory — `/api/inventory`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/inventory/products` | Public | List all 20 products with stock levels |
| POST | `/api/inventory/reserve` | Internal | Reserve stock (saga step 1) |
| POST | `/api/inventory/release` | Internal | Release reserved stock (compensation) |

### Orders — `/api/orders`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/orders` | JWT | Place order (saga starts async, returns PENDING) |
| GET | `/api/orders/{id}` | JWT | Poll order status |
| GET | `/api/orders/user/{userId}` | JWT | All orders for user |
| GET | `/api/orders` | JWT | Paginated order history with status filter |

### Payments — `/api/payments`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/payments/charge` | Internal | Charge payment (~15% failure rate) |
| GET | `/api/payments/transactions/{orderId}` | Internal | Get transaction record |

---

## Frontend Pages

| Route | Page | Description |
|---|---|---|
| `/` | LandingPage | Hero, saga flow explainer, tech cards, tech stack |
| `/catalog` | CatalogPage | Product grid with category filters, search, cart |
| `/orders` | OrdersPage | Paginated order history with status filter and detail view |
| `/auth` | AuthScreen | Redesigned dark form — explicit labels, icon-prefixed inputs, eye toggle, Google OAuth2 button, password reset link |
| `/auth/forgot-password` | ForgotPasswordPage | Request password reset email |
| `/auth/reset-password` | ResetPasswordPage | Set new password via token (with strength meter) |
| `/architecture` | ArchitecturePage | Interactive service diagram · Animate Saga button · Auto-play on load toggle (localStorage) |
| `/health` | SystemHealthPage | Live actuator health dashboard with Swagger links |

**OrderTracker** — modal that auto-opens after checkout. Polls order status every second, animates the saga progress rail (Reserve → Charge → Confirm), shows compensation animation when cancelled after stock was reserved.

**Architecture page** — interactive service map with clickable nodes showing tech details. Includes "Animate Saga" button that steps through the full request flow with glow animations. An "Auto-play on load" toggle (persisted in `localStorage`) triggers the animation automatically when the page opens.

---

## Email Notifications

| Event | Subject | Template |
|---|---|---|
| Order confirmed | `Your OrderFlow order is confirmed ✓` | `order-confirmed.html` |
| Order cancelled | `Your OrderFlow order was cancelled` | `order-cancelled.html` |
| Password reset | `Reset your OrderFlow password` | `password-reset.html` |

Templates are Thymeleaf HTML styled to match the dark UI theme. All links use `${frontendUrl}` injected from config — no hardcoded `localhost` in emails.

Enable by setting `MAIL_ENABLED=true`, `MAIL_USERNAME`, and `MAIL_PASSWORD` (Gmail App Password) in `.env`. The `FRONTEND_URL` controls links inside emails.

---

## Environment Variables

### Backend (`.env` at project root)

| Variable | Default | Description |
|---|---|---|
| `GOOGLE_CLIENT_ID` | — | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | — | Google OAuth2 client secret |
| `FRONTEND_URL` | `http://localhost:3000` | OAuth2 redirect target and email link base URL |
| `MAIL_ENABLED` | `true` | Set to `false` to skip all email sending |
| `MAIL_USERNAME` | — | Gmail address |
| `MAIL_PASSWORD` | — | Gmail App Password (not your account password) |

### Frontend (baked into JS bundle at build time by Vite)

| Variable | Default | Description |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | API Gateway URL (browser-facing) |
| `VITE_AUTH_DIRECT_URL` | `http://localhost:8081` | Auth Service direct URL for OAuth2 flow |

---

## Development Notes

**Frontend changes not showing** — the frontend container serves a pre-built static bundle compiled by Vite at `docker build` time. Changes to React source files (`.jsx`) don't hot-reload in Docker. Rebuild the container after any source change:
```bash
docker compose build frontend && docker compose up -d frontend
```
For instant HMR during development, run `npm run dev` in `frontend/` directly (port 5173) while the rest of the stack runs in Docker.

**Reseeding the catalog** — `data.sql` runs `DELETE FROM products` then re-inserts on every startup (`sql.init.mode: always`, `defer-datasource-initialization: true`). This means the catalog is always in sync with the SQL file. Product IDs change on each restart but there are no FK constraints on `Reservation.productId`.

**Payment failure rate** — ~15% of charges are randomly declined. Place a few orders to see the saga compensation path trigger. The threshold is in `PaymentService`.

**MySQL port** — mapped host `3307` → container `3306` to avoid conflicts with a local MySQL installation on Windows.

**Kafka** — KRaft mode (no Zookeeper). Single broker, replication factor 1. Both topics (`order-events`, `auth-events`) are pre-created by `KafkaTopicConfig` in notification-service at startup to avoid race conditions between producer and consumer.

**React Router future flags** — `v7_startTransition` and `v7_relativeSplatPath` are opted in via `BrowserRouter future={...}` in `main.jsx` to silence deprecation warnings ahead of the React Router v7 migration.

**Framer Motion `transparent` colors** — all `backgroundColor` animations use explicit `rgba(...)` values instead of the `'transparent'` CSS keyword. Framer Motion requires both animation endpoints to be the same color format to interpolate correctly.

**Config changes** — edit files in `config-repo/` without rebuilding images; mounted as a live volume. Changes take effect on the next service restart (`docker compose restart <service>`).

**Running one service locally** — set `spring.config.import: configserver:http://localhost:8888` and run `./mvnw spring-boot:run` from the service directory while the rest of the stack runs in Docker.

---

## Tech Stack

**Backend**
- Java 17, Spring Boot 3.2.5, Spring Cloud 2023.0.1
- Spring Cloud Gateway (WebFlux), Netflix Eureka, Spring Cloud Config
- Spring Kafka 3, Resilience4j 2.2 (circuit breaker), Spring Retry
- Spring Data JPA, MySQL 8, PostgreSQL 16
- Spring Security 6, Spring Mail, Thymeleaf 3
- Springdoc OpenAPI 2.5 (Swagger UI per service)
- Apache Kafka 3.7 (KRaft mode)

**Frontend**
- React 18, React Router 6, Vite 5
- Tailwind CSS 3, Framer Motion 11, Lucide React
- Axios 1.6

**Infrastructure**
- Docker, Docker Compose
- Nginx (frontend container static serving)
