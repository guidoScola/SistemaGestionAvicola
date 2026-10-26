# Feature: Security & Users (Module 1)
Objective: login with username/password and configurable role-based permissions. Branch: `feat/security-users`.
Delivery strategy: ask-on-risk. Test runners: `./mvnw test` (backend), `npm test` (frontend, Vitest).

## Specs
- S1: "Acceso al sistema mediante usuario y contraseña." — login, token refresh (rotating httpOnly cookie), logout.
- S2: "Niveles de usuario con distintos permisos, para definir qué puede ver y hacer cada persona." — configurable roles: admin creates roles and assigns permission codes from the UI; users may have several roles.
- S3: User CRUD (admin): create, edit, activate/deactivate (soft delete), assign roles, reset password; users change their own password.
- S4: Enforcement: API returns 403 without the permission; UI hides menus/actions the user lacks.
- S5: First admin is bootstrapped from env vars `ADMIN_USERNAME` / `ADMIN_PASSWORD`; no password in migrations. Per L11 it is a super administrator.
- S6 (L11): "pero u administrador mayor tiene que haber" — approved rules: (1) super admin = bootstrap user, holds every permission (incl. future modules) without roles; (2) only a super admin can edit, deactivate, change roles of or reset the password of a super admin; (3) users with USERS_MANAGE can only assign roles whose permissions they already hold (no escalation); (4) the last active super admin cannot be deactivated. Also: a non super admin cannot reset the password of a user holding permissions they lack.
- Assumption: root package `com.guidoscola.avicola`.
- Out of scope: account lockout, email password recovery, audit log, 2FA.

## Tasks
| ID | Specs | Unit | Route | Status | Commit |
|---|---|---|---|---|---|
| T1 | — | Repo skeleton (git, backend Boot 4.1.2, Flyway, Postgres profiles, health; Vite React-TS) | inline | done | c6e1134 |
| T2 | S2, S3 | Security domain + ports | inline | done | ef88d7f |
| T3 | S2, S5 | Persistence: Flyway V1 + seed, JPA adapters, admin bootstrap | inline | done | 4f46a20 |
| T4 | S1 | Authentication API + SecurityConfig + JWT | inline | done | 46b36f2 |
| T5 | S2, S3, S4 | Users/Roles/Permissions API with @PreAuthorize | inline | done | 66570d8 + fix |
| T6 | S1, S4 | Frontend auth, protected routes, `<Can>` | inline | pending | |
| T7 | S2, S3, S4 | Frontend users and roles screens | inline | pending | |
| T5b | S6 | Super admin + no-escalation rules | inline | done | see log L12 |
| T8 | — | README | inline | pending | |

Risk: item 2 (security) on T4/T5 → independent verifier at their close.

## Log
- L1 (2026-10-26): "Quiero armar un plan para ejecutar el primer mnodulo del sistema"
- L2 (2026-10-26): User chose "API REST + React" and "Roles configurables". Plan approved.
- L3: Environment: JDK 21.0.12, Node 24.15, npm 11.12, git 2.54, Postgres 17 running; no Docker/Maven (use wrapper).
- L4 (2026-10-26): T2 RED observed (compile errors, symbols missing), GREEN: 38 backend tests pass (domain + UserManagementService + RoleManagementService). Password rule: min 8 chars. Roles in use cannot be deleted.
- L5 (2026-10-26): T3 RED (AdminBootstrap missing), GREEN: 45 tests incl. SecurityPersistenceIT + AdminBootstrapIT against avicola_test (clean+migrate per context, truncate per test). Surefire now includes *IT. Bootstrap only when no users exist; creates role "Administrador" with full catalog.
- L6 (2026-10-26): T4 RED (AuthApiIT 7/8 failing), GREEN: 66 tests. HS256 JWT (15m, claims username/fullName/permissions, issuer avicola), refresh token 7d rotating, atomic revoke via conditional UPDATE, reuse/race revokes all user sessions. JWT_SECRET >=32 bytes required (fail fast). No CORS: Vite proxy keeps same origin. Known tradeoff: permission/deactivation changes apply to access tokens only after <=15m.
- L7 (2026-10-26): Native review review-d857c455575d80a3 over c6e1134..46b36f2 (lenses risk/resilience/reliability): APPROVED, acknowledged. 9 non-blocking advisories recorded in Engram (odd/security-users/review-1). R1-002 (password reset/change keep sessions) folded into T5 since T5 exposes those endpoints.
- L8 (2026-10-26): T5 RED (UsersApiIT 8/8, RolesApiIT 6/6 failing), GREEN: 84 tests. Password reset/change and deactivation now revoke all refresh tokens (4 unit tests written together with the fix, RED not observed separately). /api/permissions returns code+description.
- L9 (2026-10-26): Independent verifier on 66570d8: no blockers; S1, S3, S5 PASS; S2/S4 API PASS (UI pending). Advisories: roleIds [null] gave 500 (FIXED, RED observed, 85 tests green); USERS_MANAGE can self-assign any role and reset admin passwords (= admin-equivalent, product decision pending); admin can lock themselves out; reset/deactivate save + revoke not in one transaction.
- L10 (2026-10-26): User chose UI copy in Spanish ("Español"); code stays English.
- L11 (2026-10-26): Verifier advisory: USERS_MANAGE was admin-equivalent. User: "pero u administrador mayor tiene que haber"; then approved "Sí, con las 4 reglas".
- L12 (2026-10-26): T5b GREEN: 95 tests (SuperAdminRulesTest 8, UsersApiIT +2). RED not observed separately (tests and implementation written in one batch). V3 adds users.super_admin. PermissionResolver is the single source of effective permissions (auth + user management). Bootstrap no longer creates an "Administrador" role.
