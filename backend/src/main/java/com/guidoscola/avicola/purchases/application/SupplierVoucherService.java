package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.purchases.application.port.SupplierRepository;
import com.guidoscola.avicola.purchases.application.port.SupplierVoucherRepository;
import com.guidoscola.avicola.purchases.domain.model.Supplier;
import com.guidoscola.avicola.purchases.domain.model.SupplierVoucher;
import com.guidoscola.avicola.shared.domain.ConflictException;
import com.guidoscola.avicola.shared.domain.InvalidInputException;
import com.guidoscola.avicola.shared.domain.NotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Entry of supplier vouchers. The VAT purchases ledger and the supplier current accounts are derived from them,
 * so registering a voucher updates both.
 */
public class SupplierVoucherService {

    private final SupplierVoucherRepository vouchers;
    private final SupplierRepository suppliers;

    public SupplierVoucherService(SupplierVoucherRepository vouchers, SupplierRepository suppliers) {
        this.vouchers = vouchers;
        this.suppliers = suppliers;
    }

    public SupplierVoucher register(SupplierVoucherCommand command) {
        Supplier supplier = suppliers.findById(command.supplierId())
                .orElseThrow(() -> new InvalidInputException("Unknown supplier"));
        if (!supplier.isActive()) {
            throw new InvalidInputException("Supplier is inactive");
        }
        SupplierVoucher voucher = SupplierVoucher.register(supplier.id(), command.type(), command.issueDate(),
                command.dueDate(), command.pointOfSale(), command.number(), command.amounts());
        if (vouchers.exists(supplier.id(), voucher.type(), voucher.pointOfSale(), voucher.number())) {
            throw new ConflictException("Voucher " + voucher.type() + " " + voucher.formattedNumber()
                    + " is already registered for this supplier");
        }
        return vouchers.save(voucher);
    }

    public SupplierVoucher get(UUID id) {
        return vouchers.findById(id).orElseThrow(() -> new NotFoundException("Voucher not found: " + id));
    }

    public VoucherEntry entry(UUID id) {
        SupplierVoucher voucher = get(id);
        return new VoucherEntry(voucher, suppliers.findById(voucher.supplierId()).orElseThrow());
    }

    public List<VoucherEntry> list(LocalDate from, LocalDate to) {
        DateRange.requireComplete(from, to);
        return withSuppliers(vouchers.findByIssueDateBetween(from, to));
    }

    List<VoucherEntry> withSuppliers(List<SupplierVoucher> list) {
        Map<UUID, Supplier> byId = suppliers.findAll().stream()
                .collect(Collectors.toMap(Supplier::id, Function.identity()));
        return list.stream().map(v -> new VoucherEntry(v, byId.get(v.supplierId()))).toList();
    }
}
