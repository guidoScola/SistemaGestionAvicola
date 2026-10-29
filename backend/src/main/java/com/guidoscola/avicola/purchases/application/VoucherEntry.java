package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.purchases.domain.model.Supplier;
import com.guidoscola.avicola.purchases.domain.model.SupplierVoucher;

/** A voucher together with the supplier that issued it. */
public record VoucherEntry(SupplierVoucher voucher, Supplier supplier) {
}
