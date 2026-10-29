package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.purchases.application.port.SupplierRepository;
import com.guidoscola.avicola.purchases.domain.model.Cuit;
import com.guidoscola.avicola.purchases.domain.model.Supplier;
import com.guidoscola.avicola.shared.domain.ConflictException;
import com.guidoscola.avicola.shared.domain.NotFoundException;

import java.util.List;
import java.util.UUID;

/**
 * Supplier ABM. Suppliers are never deleted: "baja" deactivates them so invoices and current accounts keep their
 * reference.
 */
public class SupplierManagementService {

    private final SupplierRepository suppliers;

    public SupplierManagementService(SupplierRepository suppliers) {
        this.suppliers = suppliers;
    }

    public Supplier create(SupplierCommand command) {
        Cuit cuit = new Cuit(command.cuit());
        requireUniqueCuit(cuit, null);
        return suppliers.save(Supplier.create(command.businessName(), cuit, command.vatCondition(), command.address(),
                command.city(), command.province(), command.phone(), command.email()));
    }

    public Supplier update(UUID id, SupplierCommand command) {
        Supplier supplier = get(id);
        Cuit cuit = new Cuit(command.cuit());
        requireUniqueCuit(cuit, id);
        supplier.update(command.businessName(), cuit, command.vatCondition(), command.address(), command.city(),
                command.province(), command.phone(), command.email());
        return suppliers.save(supplier);
    }

    public void activate(UUID id) {
        Supplier supplier = get(id);
        supplier.activate();
        suppliers.save(supplier);
    }

    public void deactivate(UUID id) {
        Supplier supplier = get(id);
        supplier.deactivate();
        suppliers.save(supplier);
    }

    public Supplier get(UUID id) {
        return suppliers.findById(id).orElseThrow(() -> new NotFoundException("Supplier not found: " + id));
    }

    public List<Supplier> list() {
        return suppliers.findAll();
    }

    private void requireUniqueCuit(Cuit cuit, UUID ownId) {
        suppliers.findByCuit(cuit)
                .filter(existing -> !existing.id().equals(ownId))
                .ifPresent(existing -> {
                    throw new ConflictException("A supplier with CUIT " + cuit.formatted() + " already exists");
                });
    }
}
