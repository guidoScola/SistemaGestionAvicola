package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.purchases.domain.model.VoucherAmounts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** VAT purchases ledger of a period: one signed row per voucher and the column totals. */
public record VatLedger(LocalDate from, LocalDate to, List<VatLedgerRow> rows, VoucherAmounts totals,
                        BigDecimal total) {
}
