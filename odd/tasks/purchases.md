# Feature: Purchases and Suppliers (Module 2)
Objective: supplier management, supplier invoices, VAT purchases ledger and supplier current accounts. Branch: `feat/purchases` (stacked on `feat/security-users`).
Delivery strategy: ask-on-risk. Test runners: `./mvnw test` (backend), `npm test` (frontend, Vitest).

## Specs
- S1: "ABM (alta, baja y modificación) de proveedores." — per L2, suppliers first. Fields: business name, CUIT (11 digits, valid check digit, unique), VAT condition (Responsable Inscripto, Monotributista, Exento, No Responsable), address, city, province, phone, email. "Baja" is a soft deactivation (suppliers will be referenced by invoices) and can be reverted. Permissions `SUPPLIERS_VIEW`, `SUPPLIERS_MANAGE`. UI in Spanish. (Field list and VAT conditions are assumptions announced to the user in L2.)
- S2: "Ingreso de comprobantes de proveedores." — pending decision on supported voucher types and tax concepts (L2).
- S3: "Actualización automática del subdiario de IVA compras." — pending S2.
- S4: "Actualización automática de las cuentas corrientes de proveedores." — pending S2.

## Tasks
| ID | Specs | Unit | Route | Status | Commit |
|---|---|---|---|---|---|
| T1 | S1 | Suppliers backend: domain, V4 migration + permissions, API | inline | done | see log L3 |
| T2 | S1 | Suppliers frontend: list, create, edit, deactivate/reactivate | inline | in progress | |
| T3 | S2-S4 | Supplier invoices, VAT ledger, current account | — | blocked on decision | |

## Log
- L1 (2026-10-26): "Bien, habria que seguir con le otro modulo"
- L2 (2026-10-26): Asked which voucher types/tax concepts to support; user: "No es mejor hacer compras y prveedores, los abm y demas" -> do suppliers ABM first, defer the invoice decision.
- L3 (2026-10-26): T1 RED (106 compile errors), GREEN: 127 backend tests (CuitTest 10, SupplierManagementServiceTest 9, SuppliersApiIT 7). RolesApiIT catalog assertion relaxed from exact 4 codes to hasItems (each module adds permissions).
