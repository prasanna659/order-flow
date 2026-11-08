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
       ├──▶ Auth Service        :8081  ── PostgreSQL (authdb)
       ├──▶ Inventory Service   :8082  ── PostgreSQL (inventorydb)
       ├──▶ Order Service       :8083  ── PostgreSQL (orderdb)  ──▶ Kafka
       ├──▶ Payment Service     :8084  ── in-memory (~15% failure sim)
       └──▶ Notification Svc    :8085  ── Kafka consumer ──▶ Gmail SMTP

Platform: Eureka :8761 · Config Server :8888 · Kafka :9092
```

### Services at a glance

| Service | Port | Role | Persistence |
|---|---|---|---|
| API Gateway | 8080 | Routing, JWT filter, CORS | — |
| Auth Service | 8081 | Register / login / Google OAuth2 / password reset | PostgreSQL |
| Inventory Service | 8082 | Product catalog, stock reserve & release | PostgreSQL |
| Order Service | 8083 | Saga orchestrator, order lifecycle | PostgreSQL |
| Payment Service | 8084 | Charge simulation (~15% random decline) | In-memory |
| Notification Service | 8085 | Kafka consumer, transactional email via Gmail SMTP | — |
| Eureka Server | 8761 | Service discovery registry | — |
| Config Server | 8888 | Centralised config (serves `/config-repo/*.yml`) | — |

---

## Distributed Systems Patterns

### Saga (Orchestration)

Orders run a 3-step saga on a background async thread (`OrderSagaOrchestrator`, a separate bean so Spring's `@Async` proxy actually applies). The HTTP response returns immediately with `PENDING` — the frontend polls every second to animate status transitions.

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

- One PostgreSQL 16 server with a separate database per service: `authdb`, `orderdb`, `inventorydb`
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
- Docker and Docker Compose v2
- (Optional) Java 17 + Maven and Node 20 if you want to run tests or the frontend outside Docker

### 1. Configure `.env`

```bash
cp .env.example .env
```

Set at least `DB_PASSWORD` and `JWT_SECRET` (min 32 chars; `openssl rand -hex 64` works). Compose refuses to start without them.
Google sign-in and e-mail are optional: leave `GOOGLE_CLIENT_ID` blank to disable Google login, and keep `MAIL_ENABLED=false`
to skip e-mails (for Gmail use an App Password, never your account password).

### 2. Google OAuth2 redirect URI (only if using Google login)

In Google Cloud Console, open Credentials, then your OAuth2 client, and add this to **Authorized redirect URIs**:

```
http://localhost:8081/login/oauth2/code/google
```

### 3. Start everything

```bash
docker compose up --build
```

The first build takes a few minutes (Maven downloads dependencies). Services start in healthcheck order:

```
postgres + kafka
  -> auth, inventory, payment
    -> order
      -> api-gateway
        -> frontend
```

Open **http://localhost:3000** once the frontend is up. Eureka and Config Server are **not** needed by the app (each service has them
disabled and reads plain environment variables, the same way it does on Railway). Start them only if you want to look at them:
`docker compose --profile platform up`.

### 4. Explore

| URL | What it is |
|---|---|
| http://localhost:3000 | React app — landing, catalog, cart, orders |
| http://localhost:3000/architecture | Interactive architecture diagram |
| http://localhost:3000/health | Live health dashboard (all services + infra) |
| http://localhost:8761 | Eureka service registry (only with `--profile platform`) |
| http://localhost:8080/docs/auth/swagger-ui.html | Auth Service Swagger UI |
| http://localhost:8080/docs/orders/swagger-ui.html | Order Service Swagger UI |
| http://localhost:8080/docs/inventory/swagger-ui.html | Inventory Service Swagger UI |
| http://localhost:8080/docs/payment/swagger-ui.html | Payment Service Swagger UI |
| http://localhost:8080/docs/notification/swagger-ui.html | Notification Service Swagger UI |

---

## Project Structure

```
orderflow/
├── api-gateway/            Spring Cloud Gateway: routing, JWT filter, CORS
├── auth-service/           Register, login, Google OAuth2, password reset
├── inventory-service/      Product catalog, stock reserve/release
├── order-service/          Saga orchestrator, order CRUD
├── payment-service/        Charge simulation (15% random failure)
├── notification-service/   Kafka consumer, Thymeleaf email templates
├── eureka-server/          Netflix Eureka registry (optional, profile "platform")
├── config-server/          Spring Cloud Config Server (optional, profile "platform")
├── config-repo/            YAML served by Config Server (optional)
├── frontend/               React 18 + Vite + Tailwind SPA
├── docker/postgres-init/   Creates authdb, orderdb, inventorydb on first start
├── .github/workflows/      CI: build + test every service, build frontend, validate compose
├── docker-compose.yml      Full local stack
└── .env.example            Copy to .env (never commit .env)
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

Users can only read and create **their own** orders. The gateway puts the verified user id in `X-User-Id`; order-service returns 403 for another user's `userId` and 404 for another user's order id.

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/orders` | JWT | Place order (saga starts async, returns PENDING). `userId` must be your own |
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

### Backend (`.env` at project root; see `.env.example`)

| Variable | Required | Description |
|---|---|---|
| `DB_PASSWORD` | yes | Postgres password |
| `JWT_SECRET` | yes | Shared by auth-service and api-gateway, min 32 chars. With `SPRING_PROFILES_ACTIVE=prod`, auth-service and api-gateway refuse to start without it |
| `FRONTEND_URL` | no (`http://localhost:3000`) | CORS origin, OAuth2 redirect target, e-mail link base |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | no | Google sign-in; blank disables it |
| `MAIL_ENABLED` | no (`false` in compose) | `false` skips all e-mail |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | no | Gmail address and App Password |

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

**Payment failure rate** — ~15% of charges are randomly declined. Place a few orders to see the saga compensation path trigger. The threshold is `FAILURE_RATE` in `PaymentService`.

**Kafka** — KRaft mode (no Zookeeper). Single broker, replication factor 1. Both topics (`order-events`, `auth-events`) are pre-created by `KafkaTopicConfig` in notification-service at startup to avoid race conditions between producer and consumer.

**React Router future flags** — `v7_startTransition` and `v7_relativeSplatPath` are opted in via `BrowserRouter future={...}` in `main.jsx` to silence deprecation warnings ahead of the React Router v7 migration.

**Framer Motion `transparent` colors** — all `backgroundColor` animations use explicit `rgba(...)` values instead of the `'transparent'` CSS keyword. Framer Motion requires both animation endpoints to be the same color format to interpolate correctly.

---

## Testing and CI

Unit tests need no database, Kafka or Docker:

```bash
cd order-service && mvn test      # saga: happy path, payment decline + compensation, reservation failure,
                                  # compensation failure; Kafka publisher; order ownership rules
cd inventory-service && mvn test  # reserve / release / insufficient stock
cd payment-service && mvn test
cd api-gateway && mvn test        # JWT validation
cd auth-service && mvn test       # token issuing, secret checks
```

GitHub Actions (`.github/workflows/ci.yml`) runs `mvn verify` for every service, builds the frontend, and validates `docker-compose.yml` on each push and pull request.

---

## Deployment

The app is deployed with **Railway** (backend services, Postgres, Kafka) and **Vercel** (frontend). Each service reads plain environment variables.

**Railway: set these before deploying**

| Service | Variables |
|---|---|
| all Java services | `SPRING_PROFILES_ACTIVE=prod`, `JWT_SECRET` (identical on auth-service and api-gateway) |
| auth, order, inventory | `SPRING_DATASOURCE_URL` (`jdbc:postgresql://...`), `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| auth, order, notification | `KAFKA_BOOTSTRAP_SERVERS` |
| order-service | `INVENTORY_SERVICE_URL`, `PAYMENT_SERVICE_URL` |
| api-gateway | `AUTH_SERVICE_URL`, `ORDER_SERVICE_URL`, `INVENTORY_SERVICE_URL`, `PAYMENT_SERVICE_URL`, `NOTIFICATION_SERVICE_URL`, `FRONTEND_URL` |
| auth-service | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `OAUTH2_REDIRECT_URI` |
| notification-service | `MAIL_ENABLED`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `FRONTEND_URL` |

**Vercel:** set `VITE_API_BASE_URL` (gateway URL, with `https://`) and `VITE_AUTH_DIRECT_URL` (auth-service URL).

**Keep order-service, inventory-service and payment-service private** (Railway private networking) and expose only the gateway, plus auth-service for the Google redirect. Those services trust the `X-User-Id` header, so they must only be reachable through the gateway.

---

## Known limitations

- Hibernate `ddl-auto: update` creates the schema. For real production use, move to Flyway migrations.
- Payment is a simulation (random 15% decline, in-memory ledger).
- Product stock is re-seeded from `data.sql` on every inventory-service start.
- Compensation (`releaseStock`) failures are logged, not retried. A dead-letter queue or reconciliation job would close that gap.

---

## Tech Stack

**Backend**
- Java 17, Spring Boot 3.2.5, Spring Cloud 2023.0.1
- Spring Cloud Gateway (WebFlux), Netflix Eureka, Spring Cloud Config
- Spring Kafka 3, Resilience4j 2.2 (circuit breaker), Spring Retry
- Spring Data JPA, PostgreSQL 16
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
