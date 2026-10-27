package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.purchases.domain.model.Supplier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Current account of a supplier; the range bounds are optional. */
public record SupplierAccount(Supplier supplier, LocalDate from, LocalDate to, BigDecimal openingBalance,
                              List<AccountMovement> movements, BigDecimal closingBalance) {
}
