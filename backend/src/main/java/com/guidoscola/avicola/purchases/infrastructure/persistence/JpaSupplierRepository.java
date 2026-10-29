package com.guidoscola.avicola.purchases.infrastructure.persistence;

import com.guidoscola.avicola.purchases.application.port.SupplierRepository;
import com.guidoscola.avicola.purchases.domain.model.Cuit;
import com.guidoscola.avicola.purchases.domain.model.Supplier;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SupplierJpaRepository extends JpaRepository<SupplierEntity, UUID> {

    Optional<SupplierEntity> findByCuit(String cuit);
}

@Repository
class JpaSupplierRepository implements SupplierRepository {

    private final SupplierJpaRepository jpa;

    JpaSupplierRepository(SupplierJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Supplier save(Supplier supplier) {
        return jpa.save(SupplierEntity.fromDomain(supplier)).toDomain();
    }

    @Override
    public Optional<Supplier> findById(UUID id) {
        return jpa.findById(id).map(SupplierEntity::toDomain);
    }

    @Override
    public Optional<Supplier> findByCuit(Cuit cuit) {
        return jpa.findByCuit(cuit.value()).map(SupplierEntity::toDomain);
    }

    @Override
    public List<Supplier> findAll() {
        return jpa.findAll(Sort.by("businessName")).stream().map(SupplierEntity::toDomain).toList();
    }
}
