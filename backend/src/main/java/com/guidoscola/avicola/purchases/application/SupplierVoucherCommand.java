package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.purchases.domain.model.VoucherAmounts;
import com.guidoscola.avicola.purchases.domain.model.VoucherType;

import java.time.LocalDate;
import java.util.UUID;

public record SupplierVoucherCommand(UUID supplierId, VoucherType type, LocalDate issueDate, LocalDate dueDate,
                                     int pointOfSale, long number, VoucherAmounts amounts) {
}
