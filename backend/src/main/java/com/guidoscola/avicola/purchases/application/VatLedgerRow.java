package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.purchases.domain.model.VoucherAmounts;

import java.math.BigDecimal;

/** A ledger row; amounts and total are negative for credit notes. */
public record VatLedgerRow(VoucherEntry entry, VoucherAmounts amounts, BigDecimal total) {
}
