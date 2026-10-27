package com.guidoscola.avicola.purchases.application.port;

import com.guidoscola.avicola.purchases.domain.model.SupplierVoucher;
import com.guidoscola.avicola.purchases.domain.model.VoucherType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierVoucherRepository {

    SupplierVoucher save(SupplierVoucher voucher);

    Optional<SupplierVoucher> findById(UUID id);

    boolean exists(UUID supplierId, VoucherType type, int pointOfSale, long number);

    /** Vouchers issued within the inclusive range, ordered by issue date, point of sale and number. */
    List<SupplierVoucher> findByIssueDateBetween(LocalDate from, LocalDate to);

    /** All vouchers of a supplier, ordered by issue date, point of sale and number. */
    List<SupplierVoucher> findBySupplier(UUID supplierId);
}
