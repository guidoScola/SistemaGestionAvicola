package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.purchases.domain.model.SupplierVoucher;

import java.math.BigDecimal;

/** A current account movement with the balance owed to the supplier after it. */
public record AccountMovement(SupplierVoucher voucher, BigDecimal debit, BigDecimal credit, BigDecimal balance) {
}
