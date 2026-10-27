package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.purchases.domain.model.Supplier;
import com.guidoscola.avicola.purchases.domain.model.SupplierVoucher;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** A voucher together with the supplier that issued it. */
public record VoucherEntry(SupplierVoucher voucher, Supplier supplier) {

    static List<VoucherEntry> join(List<SupplierVoucher> vouchers, List<Supplier> suppliers) {
        Map<UUID, Supplier> byId = suppliers.stream()
                .collect(Collectors.toMap(Supplier::id, Function.identity()));
        return vouchers.stream().map(v -> new VoucherEntry(v, byId.get(v.supplierId()))).toList();
    }
}
