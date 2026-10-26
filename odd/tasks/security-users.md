# Feature: Security & Users (Module 1)
Objective: login with username/password and configurable role-based permissions. Branch: `feat/security-users`.
Delivery strategy: ask-on-risk. Test runners: `./mvnw test` (backend), `npm test` (frontend, Vitest).

## Specs
- S1: "Acceso al sistema mediante usuario y contraseña." — login, token refresh (rotating httpOnly cookie), logout.
- S2: "Niveles de usuario con distintos permisos, para definir qué puede ver y hacer cada persona." — configurable roles: admin creates roles and assigns permission codes from the UI; users may have several roles.
- S3: User CRUD (admin): create, edit, activate/deactivate (soft delete), assign roles, reset password; users change their own password.
- S4: Enforcement: API returns 403 without the permission; UI hides menus/actions the user lacks.
- S5: First admin is bootstrapped from env vars `ADMIN_USERNAME` / `ADMIN_PASSWORD`; no password in migrations.
- Assumption: root package `com.guidoscola.avicola`.
- Out of scope: account lockout, email password recovery, audit log, 2FA.

## Tasks
| ID | Specs | Unit | Route | Status | Commit |
|---|---|---|---|---|---|
| T1 | — | Repo skeleton (git, backend Boot 4.1.2, Flyway, Postgres profiles, health; Vite React-TS) | inline | done | c6e1134 |
| T2 | S2, S3 | Security domain + ports | inline | done | ef88d7f |
| T3 | S2, S5 | Persistence: Flyway V1 + seed, JPA adapters, admin bootstrap | inline | done | 4f46a20 |
| T4 | S1 | Authentication API + SecurityConfig + JWT | inline | done | see log L6 |
| T5 | S2, S3, S4 | Users/Roles/Permissions API with @PreAuthorize | inline | pending | |
| T6 | S1, S4 | Frontend auth, protected routes, `<Can>` | inline | pending | |
| T7 | S2, S3, S4 | Frontend users and roles screens | inline | pending | |
| T8 | — | README | inline | pending | |

Risk: item 2 (security) on T4/T5 → independent verifier at their close.

## Log
- L1 (2026-10-26): "Quiero armar un plan para ejecutar el primer mnodulo del sistema"
- L2 (2026-10-26): User chose "API REST + React" and "Roles configurables". Plan approved.
- L3: Environment: JDK 21.0.12, Node 24.15, npm 11.12, git 2.54, Postgres 17 running; no Docker/Maven (use wrapper).
- L4 (2026-10-26): T2 RED observed (compile errors, symbols missing), GREEN: 38 backend tests pass (domain + UserManagementService + RoleManagementService). Password rule: min 8 chars. Roles in use cannot be deleted.
- L5 (2026-10-26): T3 RED (AdminBootstrap missing), GREEN: 45 tests incl. SecurityPersistenceIT + AdminBootstrapIT against avicola_test (clean+migrate per context, truncate per test). Surefire now includes *IT. Bootstrap only when no users exist; creates role "Administrador" with full catalog.
- L6 (2026-10-26): T4 RED (AuthApiIT 7/8 failing), GREEN: 66 tests. HS256 JWT (15m, claims username/fullName/permissions, issuer avicola), refresh token 7d rotating, atomic revoke via conditional UPDATE, reuse/race revokes all user sessions. JWT_SECRET >=32 bytes required (fail fast). No CORS: Vite proxy keeps same origin. Known tradeoff: permission/deactivation changes apply to access tokens only after <=15m.
