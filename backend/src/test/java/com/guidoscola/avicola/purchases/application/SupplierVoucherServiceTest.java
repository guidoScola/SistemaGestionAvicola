package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.purchases.application.SupplierManagementServiceTest.InMemorySupplierRepository;
import com.guidoscola.avicola.purchases.application.port.SupplierVoucherRepository;
import com.guidoscola.avicola.purchases.domain.model.Supplier;
import com.guidoscola.avicola.purchases.domain.model.SupplierVoucher;
import com.guidoscola.avicola.purchases.domain.model.VatCondition;
import com.guidoscola.avicola.purchases.domain.model.VoucherAmounts;
import com.guidoscola.avicola.purchases.domain.model.VoucherType;
import com.guidoscola.avicola.shared.domain.ConflictException;
import com.guidoscola.avicola.shared.domain.InvalidInputException;
import com.guidoscola.avicola.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupplierVoucherServiceTest {

    static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    static final LocalDate OCT_31 = LocalDate.of(2026, 10, 31);

    private InMemorySupplierRepository suppliers;
    private InMemorySupplierVoucherRepository vouchers;
    private SupplierVoucherService service;
    private Supplier granja;

    @BeforeEach
    void setUp() {
        suppliers = new InMemorySupplierRepository();
        vouchers = new InMemorySupplierVoucherRepository();
        service = new SupplierVoucherService(vouchers, suppliers);
        granja = new SupplierManagementService(suppliers).create(new SupplierCommand("Granja Don Pedro SA",
                "30-71234567-1", VatCondition.RESPONSABLE_INSCRIPTO, null, null, null, null, null));
    }

    static VoucherAmounts taxed21(String net, String vat) {
        return new VoucherAmounts(new BigDecimal(net), null, null, new BigDecimal(vat), null, null, null, null, null,
                null, null);
    }

    static SupplierVoucherCommand facturaA(UUID supplierId, LocalDate issued, long number, String net, String vat) {
        return new SupplierVoucherCommand(supplierId, VoucherType.FACTURA_A, issued, issued.plusDays(30), 1, number,
                taxed21(net, vat));
    }

    @Test
    void registersVoucherForActiveSupplier() {
        SupplierVoucher voucher = service.register(facturaA(granja.id(), OCT_1.plusDays(4), 1234, "1000", "210"));

        assertThat(vouchers.findById(voucher.id())).isPresent();
        assertThat(voucher.supplierId()).isEqualTo(granja.id());
        assertThat(voucher.type()).isEqualTo(VoucherType.FACTURA_A);
        assertThat(voucher.issueDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(voucher.dueDate()).isEqualTo(LocalDate.of(2026, 11, 4));
        assertThat(voucher.total()).isEqualByComparingTo("1210");
        assertThat(service.get(voucher.id()).number()).isEqualTo(1234L);
    }

    @Test
    void rejectsUnknownAndInactiveSupplier() {
        assertThatThrownBy(() -> service.register(facturaA(UUID.randomUUID(), OCT_1, 1, "100", "21")))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Unknown supplier");

        new SupplierManagementService(suppliers).deactivate(granja.id());

        assertThatThrownBy(() -> service.register(facturaA(granja.id(), OCT_1, 1, "100", "21")))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Supplier is inactive");
        assertThat(vouchers.all).isEmpty();
    }

    @Test
    void rejectsDuplicateVoucherOfTheSameSupplier() {
        service.register(facturaA(granja.id(), OCT_1, 1234, "100", "21"));

        assertThatThrownBy(() -> service.register(facturaA(granja.id(), OCT_1.plusDays(1), 1234, "200", "42")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Voucher FACTURA_A 00001-00001234 is already registered for this supplier");

        Supplier other = new SupplierManagementService(suppliers).create(new SupplierCommand("Forrajera Sur",
                "20-12345678-6", VatCondition.MONOTRIBUTISTA, null, null, null, null, null));
        service.register(facturaA(other.id(), OCT_1, 1234, "100", "21"));
        service.register(new SupplierVoucherCommand(granja.id(), VoucherType.NOTA_CREDITO_A, OCT_1, null, 1, 1234L,
                taxed21("100", "21")));

        assertThat(vouchers.all).hasSize(3);
    }

    @Test
    void requiresAmounts() {
        assertThatThrownBy(() -> service.register(new SupplierVoucherCommand(granja.id(), VoucherType.FACTURA_A,
                OCT_1, null, 1, 1L, null)))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Amounts are required");
    }

    @Test
    void listsVouchersIssuedWithinTheRangeWithTheirSupplier() {
        service.register(facturaA(granja.id(), OCT_1.minusDays(1), 1, "100", "21"));
        SupplierVoucher october = service.register(facturaA(granja.id(), OCT_31, 2, "100", "21"));
        service.register(facturaA(granja.id(), OCT_31.plusDays(1), 3, "100", "21"));

        List<VoucherEntry> entries = service.list(OCT_1, OCT_31);

        assertThat(entries).extracting(e -> e.voucher().id()).containsExactly(october.id());
        assertThat(entries.getFirst().supplier().businessName()).isEqualTo("Granja Don Pedro SA");
    }

    @Test
    void rangeMustBeCompleteAndOrdered() {
        assertThatThrownBy(() -> service.list(null, OCT_31)).isInstanceOf(InvalidInputException.class)
                .hasMessage("Date range is required");
        assertThatThrownBy(() -> service.list(OCT_31, OCT_1)).isInstanceOf(InvalidInputException.class)
                .hasMessage("Start date must not be after end date");
    }

    @Test
    void getUnknownVoucherIsNotFound() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }

    static class InMemorySupplierVoucherRepository implements SupplierVoucherRepository {

        static final Comparator<SupplierVoucher> ORDER = Comparator.comparing(SupplierVoucher::issueDate)
                .thenComparing(SupplierVoucher::pointOfSale)
                .thenComparing(SupplierVoucher::number);

        final Map<UUID, SupplierVoucher> all = new LinkedHashMap<>();

        @Override
        public SupplierVoucher save(SupplierVoucher voucher) {
            all.put(voucher.id(), voucher);
            return voucher;
        }

        @Override
        public Optional<SupplierVoucher> findById(UUID id) {
            return Optional.ofNullable(all.get(id));
        }

        @Override
        public boolean exists(UUID supplierId, VoucherType type, int pointOfSale, long number) {
            return all.values().stream().anyMatch(v -> v.supplierId().equals(supplierId) && v.type() == type
                    && v.pointOfSale() == pointOfSale && v.number() == number);
        }

        @Override
        public List<SupplierVoucher> findByIssueDateBetween(LocalDate from, LocalDate to) {
            return all.values().stream()
                    .filter(v -> !v.issueDate().isBefore(from) && !v.issueDate().isAfter(to))
                    .sorted(ORDER)
                    .toList();
        }

        @Override
        public List<SupplierVoucher> findBySupplier(UUID supplierId) {
            return all.values().stream().filter(v -> v.supplierId().equals(supplierId)).sorted(ORDER).toList();
        }
    }
}
