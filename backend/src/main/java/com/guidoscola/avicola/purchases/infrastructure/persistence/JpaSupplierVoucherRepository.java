package com.guidoscola.avicola.purchases.infrastructure.persistence;

import com.guidoscola.avicola.purchases.application.port.SupplierVoucherRepository;
import com.guidoscola.avicola.purchases.domain.model.SupplierVoucher;
import com.guidoscola.avicola.purchases.domain.model.VoucherType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SupplierVoucherJpaRepository extends JpaRepository<SupplierVoucherEntity, UUID> {

    boolean existsBySupplierIdAndTypeAndPointOfSaleAndNumber(UUID supplierId, VoucherType type, int pointOfSale,
                                                            long number);

    List<SupplierVoucherEntity> findByIssueDateBetween(LocalDate from, LocalDate to, Sort sort);

    List<SupplierVoucherEntity> findBySupplierId(UUID supplierId, Sort sort);
}

@Repository
class JpaSupplierVoucherRepository implements SupplierVoucherRepository {

    private static final Sort ORDER = Sort.by("issueDate", "pointOfSale", "number");

    private final SupplierVoucherJpaRepository jpa;

    JpaSupplierVoucherRepository(SupplierVoucherJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public SupplierVoucher save(SupplierVoucher voucher) {
        return jpa.save(SupplierVoucherEntity.fromDomain(voucher)).toDomain();
    }

    @Override
    public Optional<SupplierVoucher> findById(UUID id) {
        return jpa.findById(id).map(SupplierVoucherEntity::toDomain);
    }

    @Override
    public boolean exists(UUID supplierId, VoucherType type, int pointOfSale, long number) {
        return jpa.existsBySupplierIdAndTypeAndPointOfSaleAndNumber(supplierId, type, pointOfSale, number);
    }

    @Override
    public List<SupplierVoucher> findByIssueDateBetween(LocalDate from, LocalDate to) {
        return jpa.findByIssueDateBetween(from, to, ORDER).stream().map(SupplierVoucherEntity::toDomain).toList();
    }

    @Override
    public List<SupplierVoucher> findBySupplier(UUID supplierId) {
        return jpa.findBySupplierId(supplierId, ORDER).stream().map(SupplierVoucherEntity::toDomain).toList();
    }
}
