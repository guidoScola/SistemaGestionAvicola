package com.guidoscola.avicola.purchases.application.port;

import com.guidoscola.avicola.purchases.domain.model.Cuit;
import com.guidoscola.avicola.purchases.domain.model.Supplier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository {

    Supplier save(Supplier supplier);

    Optional<Supplier> findById(UUID id);

    Optional<Supplier> findByCuit(Cuit cuit);

    /** All suppliers, ordered by business name. */
    List<Supplier> findAll();
}
