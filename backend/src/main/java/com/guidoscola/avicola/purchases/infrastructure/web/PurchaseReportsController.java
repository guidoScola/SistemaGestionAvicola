package com.guidoscola.avicola.purchases.infrastructure.web;

import com.guidoscola.avicola.purchases.application.AccountMovement;
import com.guidoscola.avicola.purchases.application.PurchaseReportsService;
import com.guidoscola.avicola.purchases.application.SupplierAccount;
import com.guidoscola.avicola.purchases.application.VatLedger;
import com.guidoscola.avicola.purchases.application.VatLedgerRow;
import com.guidoscola.avicola.purchases.domain.model.Supplier;
import com.guidoscola.avicola.purchases.domain.model.SupplierVoucher;
import com.guidoscola.avicola.purchases.domain.model.VatCondition;
import com.guidoscola.avicola.purchases.domain.model.VoucherAmounts;
import com.guidoscola.avicola.purchases.domain.model.VoucherType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/purchases")
class PurchaseReportsController {

    private final PurchaseReportsService reports;

    PurchaseReportsController(PurchaseReportsService reports) {
        this.reports = reports;
    }

    record LedgerRowResponse(UUID voucherId, LocalDate issueDate, VoucherType type, String formattedNumber,
                             String supplierName, String supplierCuit, VatCondition vatCondition,
                             VoucherAmounts amounts, BigDecimal total) {

        static LedgerRowResponse from(VatLedgerRow row) {
            SupplierVoucher v = row.entry().voucher();
            Supplier s = row.entry().supplier();
            return new LedgerRowResponse(v.id(), v.issueDate(), v.type(), v.formattedNumber(), s.businessName(),
                    s.cuit().formatted(), s.vatCondition(), row.amounts(), row.total());
        }
    }

    record LedgerResponse(LocalDate from, LocalDate to, List<LedgerRowResponse> rows, VoucherAmounts totals,
                          BigDecimal total) {

        static LedgerResponse from(VatLedger ledger) {
            return new LedgerResponse(ledger.from(), ledger.to(),
                    ledger.rows().stream().map(LedgerRowResponse::from).toList(), ledger.totals(), ledger.total());
        }
    }

    record MovementResponse(UUID voucherId, LocalDate issueDate, LocalDate dueDate, VoucherType type,
                            String formattedNumber, BigDecimal debit, BigDecimal credit, BigDecimal balance) {

        static MovementResponse from(AccountMovement m) {
            SupplierVoucher v = m.voucher();
            return new MovementResponse(v.id(), v.issueDate(), v.dueDate(), v.type(), v.formattedNumber(), m.debit(),
                    m.credit(), m.balance());
        }
    }

    record AccountResponse(UUID supplierId, String supplierName, String supplierCuit, LocalDate from, LocalDate to,
                           BigDecimal openingBalance, List<MovementResponse> movements, BigDecimal closingBalance) {

        static AccountResponse from(SupplierAccount account) {
            Supplier s = account.supplier();
            return new AccountResponse(s.id(), s.businessName(), s.cuit().formatted(), account.from(), account.to(),
                    account.openingBalance(), account.movements().stream().map(MovementResponse::from).toList(),
                    account.closingBalance());
        }
    }

    @GetMapping("/vat-ledger")
    @PreAuthorize("hasAuthority('PURCHASES_VIEW')")
    LedgerResponse vatLedger(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return LedgerResponse.from(reports.vatLedger(from, to));
    }

    @GetMapping("/suppliers/{id}/account")
    @PreAuthorize("hasAuthority('PURCHASES_VIEW')")
    AccountResponse supplierAccount(@PathVariable UUID id,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return AccountResponse.from(reports.supplierAccount(id, from, to));
    }
}
