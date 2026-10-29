# Feature: Purchases and Suppliers (Module 2)
Objective: supplier management, supplier invoices, VAT purchases ledger and supplier current accounts. Branch: `feat/purchases` (stacked on `feat/security-users`).
Delivery strategy: ask-on-risk. Test runners: `./mvnw test` (backend), `npm test` (frontend, Vitest).

## Specs
- S1: "ABM (alta, baja y modificación) de proveedores." — per L2, suppliers first. Fields: business name, CUIT (11 digits, valid check digit, unique), VAT condition (Responsable Inscripto, Monotributista, Exento, No Responsable), address, city, province, phone, email. "Baja" is a soft deactivation (suppliers will be referenced by invoices) and can be reverted. Permissions `SUPPLIERS_VIEW`, `SUPPLIERS_MANAGE`. UI in Spanish. (Field list and VAT conditions are assumptions announced to the user in L2.)
- S2: "Ingreso de comprobantes de proveedores." — per L7 (standard accepted): voucher types Factura A/B/C, Nota de Crédito A/B/C, Nota de Débito A/B/C; amounts: net taxed per rate (21%, 10,5%, 27%), non-taxed, exempt, VAT per rate, VAT perceptions, IIBB perceptions, other taxes, total; data: date, point of sale, number, supplier, due date. Permissions `PURCHASES_VIEW`, `PURCHASES_MANAGE`. Assumptions: only letter A discriminates VAT (B/C carry no net taxed/VAT); VAT per rate must match net × rate within $1.00 (multi-line rounding); total is computed; supplier + type + point of sale + number is unique; inactive suppliers cannot receive new vouchers; amounts are entered positive and credit notes subtract.
- S3: "Actualización automática del subdiario de IVA compras." — derived from the vouchers (no separate table), filtered by issue date range, credit notes signed negative, with column totals. Assumption: period by issue date. PDF/Excel export belongs to module 5.
- S4: "Actualización automática de las cuentas corrientes de proveedores." — derived per supplier from its vouchers: opening balance, movements with running balance (invoices/debit notes increase the debt, credit notes decrease it), closing balance. Payments join in module 3.

## Tasks
| ID | Specs | Unit | Route | Status | Commit |
|---|---|---|---|---|---|
| T1 | S1 | Suppliers backend: domain, V4 migration + permissions, API | inline | done | cc264fe |
| T2 | S1 | Suppliers frontend: list, create, edit, deactivate/reactivate | inline | done | see log L4 |
| T3 | S2 | Vouchers backend: domain, V5 migration + permissions, entry/list/get API | inline | done | see L8 |
| T4 | S3, S4 | VAT ledger and supplier current account queries + API | inline | done | see L9 |
| T5 | S2 | Vouchers frontend: list and entry form | inline | done | see L10 |
| T6 | S3, S4 | VAT ledger and current account screens | inline | done | see L10 |

## Log
- L1 (2026-10-26): "Bien, habria que seguir con le otro modulo"
- L2 (2026-10-26): Asked which voucher types/tax concepts to support; user: "No es mejor hacer compras y prveedores, los abm y demas" -> do suppliers ABM first, defer the invoice decision.
- L3 (2026-10-26): T1 RED (106 compile errors), GREEN: 127 backend tests (CuitTest 10, SupplierManagementServiceTest 9, SuppliersApiIT 7). RolesApiIT catalog assertion relaxed from exact 4 codes to hasItems (each module adds permissions).
- L4 (2026-10-26): T2 RED (7 failing), GREEN: 35 Vitest tests, lint/build clean. Suppliers page with search by name/CUIT, Spanish VAT labels and errors, menu entry gated by SUPPLIERS_VIEW. Smoke on dev: V4 migrated, super admin got SUPPLIERS_* automatically, create 201, invalid CUIT 400.
- L5 (2026-10-26): Native review of suppliers slice (83b253e..2391515, lenses risk/reliability): APPROVED, acknowledged. Advisories: concurrent same-CUIT create -> already mapped to 409 by ApiExceptionHandler (DataIntegrityViolationException, base code); catalog test uses hasItems instead of exact set; row buttons not disabled while a mutation is pending.
- L6 (2026-10-27): "sigamso con el desarrollo real. sigamso con le modulo"
- L7 (2026-10-27): Proposed the standard Argentine voucher set and tax concepts; user: "si esta bien el  estandar". T3 split into T3-T6, all inline (no Writer trigger: backend and frontend depend on the same contract).
- L8 (2026-10-27): T3 RED (172 compile errors), GREEN: SupplierVoucherTest 7, SupplierVoucherServiceTest 7, SupplierVouchersApiIT 4; full backend suite 145 tests, 0 failures. Endpoints: POST/GET /api/purchases/vouchers (from,to required), GET /api/purchases/vouchers/{id}.
- L9 (2026-10-27): T4 RED (20 compile errors), GREEN: PurchaseReportsServiceTest 6, PurchaseReportsApiIT 3; full backend suite 154 tests, 0 failures. Ledger and account are derived queries (no posting table); README API table updated.
- L10 (2026-10-27): T5+T6 RED (8 failing), GREEN: VouchersPage.test 6, PurchaseReports.test 2; full Vitest 43 tests, lint clean, build ok. Screens: /compras/comprobantes (current month, entry form suggests VAT from net, hides VAT fields for B/C, live total), /compras/iva-compras (signed rows + totals), /compras/cuenta-corriente (year to date, includes inactive suppliers). Menu gated by PURCHASES_VIEW; voucher form and account need SUPPLIERS_VIEW to load the supplier list.
- L11 (2026-10-27): Independent verifier: S2/S3/S4 PASS, no blockers (probes: 10.5% rounding, tolerance edge 22.00/22.01, rejected POSTs keep row count, from==to ledger, negative balances). Native review 0080e3b..c9a329a (lenses risk/reliability, user granted): APPROVED, acknowledged. Advisories for later: bound amount magnitude before setScale (huge exponent / >13 integer digits -> CPU cost or misleading 409); PURCHASES_MANAGE without SUPPLIERS_VIEW gets 403 in voucher form and account page; VAT accepted with zero net within the $1.00 tolerance; inverted range in UI shows no message. Open product question: voucher annulment/deletion.
- L12 (2026-10-27): Asked about voucher annulment/deletion; user: "dejalo asi por ahora" -> vouchers stay immutable (no edit, annul or delete) for now.
- L13 (2026-10-27): User: "dale" (fix review advisories 1 and 2). RED 4 failing (API IT took 27s on 1e999999999), GREEN: amounts checked for <= 13 integer digits before setScale (400, Spanish message); 'Nuevo comprobante' needs PURCHASES_MANAGE + SUPPLIERS_VIEW, current account menu/route needs PURCHASES_VIEW + SUPPLIERS_VIEW. Backend 155 tests, Vitest 45, lint/build clean.
- L14 (2026-10-29): User: "sigamnos" (fix review advisories 3 and 4). RED: SupplierVoucherTest.vatRequiresANetTaxedAmount and inverted-period test failing. GREEN: VAT with zero net rejected with "VAT <rate> requires a net taxed amount" (Spanish mapping added); PeriodFilter shows an alert for an inverted range on the three purchases screens. Backend 156 tests, Vitest 46, lint/build clean.
- L15 (2026-10-29): User chose stacked PRs to main. Chain #1-#14 (branches stack/01..14) cut at existing commits: #1 e615906, #2 e57c2b3, #3 c4e1f39, #4 cb5dbcb, #5 2dce1dd, #6 1dc1c99, #7 bf37d67, #8 83b253e, #9 cc264fe, #10 0080e3b, #11 c96812a, #12 cd135b2, #13 cc48f6d, #14 f1999f6 + this log. 11 PRs exceed 400 lines (single commits with their tests): size:exception requested in each body. Assess since c9a329a: medium, under_budget (no native review due).
