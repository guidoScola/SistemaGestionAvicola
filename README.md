# Sistema de Gestión Avícola

Management system for purchases, sales and ARCA electronic invoicing, built as a monorepo:

- `backend/` — Java 21, Spring Boot 4, PostgreSQL, Flyway, hexagonal architecture.
- `frontend/` — React 19, TypeScript, Vite, TanStack Query.

## Modules

| # | Module | Status |
|---|---|---|
| 1 | Security and users: login, configurable roles and permissions | Done |
| 2 | Purchases and suppliers | Suppliers done; invoices, VAT ledger and current accounts in progress |
| 3 | Payment orders and withholdings | Planned |
| 4 | Sales, customers and ARCA e-invoicing | Planned |
| 5 | Reports (PDF, Excel, email) | Planned |

## Requirements

- JDK 21
- Node.js 24+
- PostgreSQL 17 running locally, with two databases:

```sql
CREATE DATABASE avicola;
CREATE DATABASE avicola_test;
```

## Running locally

### Backend (port 8080)

```bash
cd backend
JWT_SECRET="<at least 32 random characters>" \
ADMIN_USERNAME=admin \
ADMIN_PASSWORD="<at least 8 characters>" \
COOKIE_SECURE=false \
./mvnw spring-boot:run
```

On Windows PowerShell, set each variable with `$env:NAME="value"` before running `.\mvnw.cmd spring-boot:run`.

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `JWT_SECRET` | yes | — | HMAC key for access tokens. The app refuses to start if it is shorter than 32 bytes. |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | first start | — | Creates the super administrator when the database has no users. Ignored afterwards. |
| `DB_URL` | no | `jdbc:postgresql://localhost:5432/avicola` | Database URL. |
| `DB_USERNAME` / `DB_PASSWORD` | no | `postgres` / `postgres` | Database credentials (development defaults). |
| `COOKIE_SECURE` | no | `true` | Marks the refresh cookie `Secure`. Set `false` only for plain-HTTP local development. |

### Frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The Vite dev server proxies `/api` to the backend, so both run on the same origin.

## Tests

```bash
cd backend && ./mvnw test     # unit + integration tests (uses the avicola_test database)
cd frontend && npm test        # Vitest + Testing Library
```

Integration tests clean and migrate `avicola_test` on every run.

## Security model

- **Login** returns a short-lived JWT access token (15 minutes) kept in memory, plus a refresh token in an
  `HttpOnly`, `SameSite=Strict` cookie scoped to `/api/auth` (7 days). Refresh tokens rotate on every use; reusing an
  old one revokes every session of that user.
- **Permissions** are codes such as `USERS_VIEW` or `ROLES_MANAGE`, grouped into roles that administrators configure.
  A user can hold several roles. Endpoints check permissions with `@PreAuthorize`; the UI hides what the user cannot do.
- **Super administrator**: the user created at first start holds every permission, including those added by future
  modules. Only a super administrator can modify a super administrator, and the last active one cannot be
  deactivated.
- **No privilege escalation**: users who are not super administrators can only assign roles, or grant role
  permissions, that they already hold.
- Changing or resetting a password, or deactivating a user, ends all of that user's sessions.

Known limitation: permission or status changes reach an already issued access token only when it expires (at most
15 minutes).

## API overview

| Method | Path | Permission |
|---|---|---|
| POST | `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout` | public |
| GET | `/api/auth/me` | authenticated |
| GET | `/api/users`, `/api/users/{id}` | `USERS_VIEW` |
| POST, PUT | `/api/users`, `/api/users/{id}`, `/api/users/{id}/activate`, `/api/users/{id}/deactivate`, `/api/users/{id}/password` | `USERS_MANAGE` |
| PUT | `/api/users/me/password` | authenticated |
| GET | `/api/roles`, `/api/roles/{id}`, `/api/permissions` | `ROLES_VIEW` |
| POST, PUT, DELETE | `/api/roles`, `/api/roles/{id}` | `ROLES_MANAGE` |
| GET | `/api/suppliers`, `/api/suppliers/{id}` | `SUPPLIERS_VIEW` |
| POST, PUT | `/api/suppliers`, `/api/suppliers/{id}`, `/api/suppliers/{id}/activate`, `/api/suppliers/{id}/deactivate` | `SUPPLIERS_MANAGE` |
| GET | `/api/purchases/vouchers?from&to`, `/api/purchases/vouchers/{id}` | `PURCHASES_VIEW` |
| POST | `/api/purchases/vouchers` | `PURCHASES_MANAGE` |
| GET | `/api/purchases/vat-ledger?from&to` (VAT purchases ledger, credit notes negative) | `PURCHASES_VIEW` |
| GET | `/api/purchases/suppliers/{id}/account?from&to` (current account; range optional) | `PURCHASES_VIEW` |

Supplier vouchers: Factura, Nota de Débito and Nota de Crédito, letters A, B and C. Only A vouchers carry net taxed
amounts and VAT (21%, 10.5%, 27%); each VAT amount must match its net × rate within $1.00. The VAT ledger and the
current accounts are computed from the vouchers, so they update as soon as a voucher is registered.

Errors use RFC 9457 problem details (`application/problem+json`).

## Development workflow

Features are tracked in `odd/tasks/<feature>.md` (specs, tasks and a decision log). Commits follow
[Conventional Commits](https://www.conventionalcommits.org/).

GitHub Actions (`.github/workflows/ci.yml`) runs both suites, plus frontend lint and build, on every pull request and on pushes to `main`. The backend job uses a PostgreSQL 17 service container for `avicola_test`.
