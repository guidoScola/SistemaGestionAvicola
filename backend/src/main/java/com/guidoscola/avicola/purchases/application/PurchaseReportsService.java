package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.purchases.application.port.SupplierRepository;
import com.guidoscola.avicola.purchases.application.port.SupplierVoucherRepository;
import com.guidoscola.avicola.purchases.domain.model.Supplier;
import com.guidoscola.avicola.purchases.domain.model.SupplierVoucher;
import com.guidoscola.avicola.purchases.domain.model.VoucherAmounts;
import com.guidoscola.avicola.shared.domain.NotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The VAT purchases ledger and the supplier current accounts. Both are computed from the registered vouchers, so
 * they are always up to date without a separate posting step.
 */
public class PurchaseReportsService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final SupplierVoucherRepository vouchers;
    private final SupplierRepository suppliers;

    public PurchaseReportsService(SupplierVoucherRepository vouchers, SupplierRepository suppliers) {
        this.vouchers = vouchers;
        this.suppliers = suppliers;
    }

    public VatLedger vatLedger(LocalDate from, LocalDate to) {
        DateRange.requireComplete(from, to);
        List<VatLedgerRow> rows = VoucherEntry.join(vouchers.findByIssueDateBetween(from, to), suppliers.findAll())
                .stream()
                .map(entry -> new VatLedgerRow(entry, entry.voucher().signedAmounts(), entry.voucher().signedTotal()))
                .toList();
        VoucherAmounts totals = rows.stream().map(VatLedgerRow::amounts)
                .reduce(VoucherAmounts.ZERO, VoucherAmounts::plus);
        return new VatLedger(from, to, rows, totals, totals.total());
    }

    /**
     * Movements of the supplier within the optional range. The balance is what is owed to the supplier: invoices
     * and debit notes are credits that raise it, credit notes are debits that lower it.
     */
    public SupplierAccount supplierAccount(UUID supplierId, LocalDate from, LocalDate to) {
        DateRange.requireOrdered(from, to);
        Supplier supplier = suppliers.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Supplier not found: " + supplierId));
        BigDecimal opening = ZERO;
        BigDecimal balance = ZERO;
        List<AccountMovement> movements = new ArrayList<>();
        for (SupplierVoucher voucher : vouchers.findBySupplier(supplierId)) {
            if (from != null && voucher.issueDate().isBefore(from)) {
                opening = opening.add(voucher.signedTotal());
                balance = opening;
            } else if (to == null || !voucher.issueDate().isAfter(to)) {
                BigDecimal amount = voucher.total();
                boolean debit = voucher.type().isCreditNote();
                balance = balance.add(voucher.signedTotal());
                movements.add(new AccountMovement(voucher, debit ? amount : ZERO, debit ? ZERO : amount, balance));
            }
        }
        return new SupplierAccount(supplier, from, to, opening, movements, balance);
    }
}
