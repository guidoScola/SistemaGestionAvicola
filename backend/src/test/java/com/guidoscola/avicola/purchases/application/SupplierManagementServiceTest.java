package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.purchases.application.port.SupplierRepository;
import com.guidoscola.avicola.purchases.domain.model.Cuit;
import com.guidoscola.avicola.purchases.domain.model.Supplier;
import com.guidoscola.avicola.purchases.domain.model.VatCondition;
import com.guidoscola.avicola.shared.domain.ConflictException;
import com.guidoscola.avicola.shared.domain.InvalidInputException;
import com.guidoscola.avicola.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupplierManagementServiceTest {

    private InMemorySupplierRepository suppliers;
    private SupplierManagementService service;

    @BeforeEach
    void setUp() {
        suppliers = new InMemorySupplierRepository();
        service = new SupplierManagementService(suppliers);
    }

    private static SupplierCommand granja(String cuit) {
        return new SupplierCommand("Granja Don Pedro SA", cuit, VatCondition.RESPONSABLE_INSCRIPTO,
                "Ruta 2 km 40", "Chascomús", "Buenos Aires", "02241-123456", "ventas@donpedro.com.ar");
    }

    @Test
    void createsActiveSupplierWithNormalizedCuit() {
        Supplier supplier = service.create(granja("30-71234567-1"));

        assertThat(suppliers.findById(supplier.id())).isPresent();
        assertThat(supplier.cuit()).isEqualTo(new Cuit("30712345671"));
        assertThat(supplier.businessName()).isEqualTo("Granja Don Pedro SA");
        assertThat(supplier.vatCondition()).isEqualTo(VatCondition.RESPONSABLE_INSCRIPTO);
        assertThat(supplier.city()).isEqualTo("Chascomús");
        assertThat(supplier.isActive()).isTrue();
    }

    @Test
    void optionalFieldsMayBeBlank() {
        Supplier supplier = service.create(new SupplierCommand("Forrajera Sur", "20-12345678-6",
                VatCondition.MONOTRIBUTISTA, " ", null, "", null, ""));

        assertThat(supplier.address()).isNull();
        assertThat(supplier.city()).isNull();
        assertThat(supplier.email()).isNull();
    }

    @Test
    void rejectsDuplicatedCuit() {
        service.create(granja("30-71234567-1"));

        assertThatThrownBy(() -> service.create(granja("30712345671"))).isInstanceOf(ConflictException.class);
        assertThat(suppliers.findAll()).hasSize(1);
    }

    @Test
    void requiresBusinessNameAndVatCondition() {
        assertThatThrownBy(() -> service.create(new SupplierCommand(" ", "30-71234567-1",
                VatCondition.EXENTO, null, null, null, null, null)))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.create(new SupplierCommand("Granja", "30-71234567-1",
                null, null, null, null, null, null)))
                .isInstanceOf(InvalidInputException.class);
        assertThat(suppliers.findAll()).isEmpty();
    }

    @Test
    void rejectsMalformedEmail() {
        assertThatThrownBy(() -> service.create(new SupplierCommand("Granja", "30-71234567-1",
                VatCondition.EXENTO, null, null, null, null, "no-es-un-mail")))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void updatesDataKeepingOwnCuit() {
        Supplier supplier = service.create(granja("30-71234567-1"));

        Supplier updated = service.update(supplier.id(), new SupplierCommand("Granja Don Pedro SRL",
                "30-71234567-1", VatCondition.EXENTO, null, "Lezama", "Buenos Aires", null, null));

        assertThat(updated.businessName()).isEqualTo("Granja Don Pedro SRL");
        assertThat(updated.vatCondition()).isEqualTo(VatCondition.EXENTO);
        assertThat(updated.city()).isEqualTo("Lezama");
        assertThat(updated.address()).isNull();
    }

    @Test
    void updateRejectsCuitOfAnotherSupplier() {
        service.create(granja("30-71234567-1"));
        Supplier other = service.create(granja("20-12345678-6"));

        assertThatThrownBy(() -> service.update(other.id(), granja("30-71234567-1")))
                .isInstanceOf(ConflictException.class);
        assertThat(suppliers.findById(other.id()).orElseThrow().cuit()).isEqualTo(new Cuit("20123456786"));
    }

    @Test
    void deactivatesAndReactivates() {
        Supplier supplier = service.create(granja("30-71234567-1"));

        service.deactivate(supplier.id());
        assertThat(suppliers.findById(supplier.id()).orElseThrow().isActive()).isFalse();

        service.activate(supplier.id());
        assertThat(suppliers.findById(supplier.id()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void failsForUnknownSupplier() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.deactivate(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }

    static class InMemorySupplierRepository implements SupplierRepository {

        private final Map<UUID, Supplier> suppliers = new LinkedHashMap<>();

        @Override
        public Supplier save(Supplier supplier) {
            suppliers.put(supplier.id(), supplier);
            return supplier;
        }

        @Override
        public Optional<Supplier> findById(UUID id) {
            return Optional.ofNullable(suppliers.get(id));
        }

        @Override
        public Optional<Supplier> findByCuit(Cuit cuit) {
            return suppliers.values().stream().filter(s -> s.cuit().equals(cuit)).findFirst();
        }

        @Override
        public List<Supplier> findAll() {
            return suppliers.values().stream().sorted(Comparator.comparing(Supplier::businessName)).toList();
        }
    }
}
