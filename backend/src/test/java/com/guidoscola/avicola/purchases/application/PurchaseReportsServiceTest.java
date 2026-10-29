package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.purchases.application.SupplierManagementServiceTest.InMemorySupplierRepository;
import com.guidoscola.avicola.purchases.application.SupplierVoucherServiceTest.InMemorySupplierVoucherRepository;
import com.guidoscola.avicola.purchases.domain.model.Supplier;
import com.guidoscola.avicola.purchases.domain.model.SupplierVoucher;
import com.guidoscola.avicola.purchases.domain.model.VatCondition;
import com.guidoscola.avicola.purchases.domain.model.VoucherAmounts;
import com.guidoscola.avicola.purchases.domain.model.VoucherType;
import com.guidoscola.avicola.shared.domain.InvalidInputException;
import com.guidoscola.avicola.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.guidoscola.avicola.purchases.application.SupplierVoucherServiceTest.OCT_1;
import static com.guidoscola.avicola.purchases.application.SupplierVoucherServiceTest.OCT_31;
import static com.guidoscola.avicola.purchases.application.SupplierVoucherServiceTest.taxed21;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseReportsServiceTest {

    private SupplierVoucherService vouchers;
    private PurchaseReportsService reports;
    private Supplier granja;
    private Supplier forrajera;

    @BeforeEach
    void setUp() {
        InMemorySupplierRepository suppliers = new InMemorySupplierRepository();
        InMemorySupplierVoucherRepository repository = new InMemorySupplierVoucherRepository();
        vouchers = new SupplierVoucherService(repository, suppliers);
        reports = new PurchaseReportsService(repository, suppliers);
        SupplierManagementService management = new SupplierManagementService(suppliers);
        granja = management.create(new SupplierCommand("Granja Don Pedro SA", "30-71234567-1",
                VatCondition.RESPONSABLE_INSCRIPTO, null, null, null, null, null));
        forrajera = management.create(new SupplierCommand("Forrajera Sur", "20-12345678-6",
                VatCondition.MONOTRIBUTISTA, null, null, null, null, null));
    }

    private SupplierVoucher register(Supplier supplier, VoucherType type, LocalDate issued, long number,
                                     VoucherAmounts amounts) {
        return vouchers.register(new SupplierVoucherCommand(supplier.id(), type, issued, null, 1, number, amounts));
    }

    private static VoucherAmounts nonTaxed(String amount) {
        return new VoucherAmounts(null, null, null, null, null, null, new BigDecimal(amount), null, null, null, null);
    }

    @Test
    void vatLedgerListsVouchersOfThePeriodWithCreditNotesNegative() {
        register(granja, VoucherType.FACTURA_A, OCT_1.minusDays(1), 1, taxed21("999", "209.79"));
        SupplierVoucher invoice = register(granja, VoucherType.FACTURA_A, OCT_1, 2, taxed21("1000", "210"));
        register(forrajera, VoucherType.FACTURA_C, OCT_1.plusDays(9), 7, nonTaxed("500"));
        register(granja, VoucherType.NOTA_CREDITO_A, OCT_31, 3, taxed21("100", "21"));
        register(granja, VoucherType.FACTURA_A, OCT_31.plusDays(1), 4, taxed21("1", "0.21"));

        VatLedger ledger = reports.vatLedger(OCT_1, OCT_31);

        assertThat(ledger.from()).isEqualTo(OCT_1);
        assertThat(ledger.to()).isEqualTo(OCT_31);
        assertThat(ledger.rows()).hasSize(3);
        VatLedgerRow first = ledger.rows().getFirst();
        assertThat(first.entry().voucher().id()).isEqualTo(invoice.id());
        assertThat(first.entry().supplier().businessName()).isEqualTo("Granja Don Pedro SA");
        assertThat(first.total()).isEqualByComparingTo("1210");
        VatLedgerRow creditNote = ledger.rows().getLast();
        assertThat(creditNote.amounts().netTaxed21()).isEqualByComparingTo("-100");
        assertThat(creditNote.amounts().vat21()).isEqualByComparingTo("-21");
        assertThat(creditNote.total()).isEqualByComparingTo("-121");

        assertThat(ledger.totals().netTaxed21()).isEqualByComparingTo("900");
        assertThat(ledger.totals().vat21()).isEqualByComparingTo("189");
        assertThat(ledger.totals().nonTaxed()).isEqualByComparingTo("500");
        assertThat(ledger.total()).isEqualByComparingTo("1589");
    }

    @Test
    void emptyVatLedgerHasZeroTotals() {
        VatLedger ledger = reports.vatLedger(OCT_1, OCT_31);

        assertThat(ledger.rows()).isEmpty();
        assertThat(ledger.totals()).isEqualTo(VoucherAmounts.ZERO);
        assertThat(ledger.total()).isEqualByComparingTo("0");
    }

    @Test
    void vatLedgerRequiresAnOrderedRange() {
        assertThatThrownBy(() -> reports.vatLedger(OCT_1, null)).isInstanceOf(InvalidInputException.class)
                .hasMessage("Date range is required");
        assertThatThrownBy(() -> reports.vatLedger(OCT_31, OCT_1)).isInstanceOf(InvalidInputException.class)
                .hasMessage("Start date must not be after end date");
    }

    @Test
    void currentAccountCarriesOpeningRunningAndClosingBalances() {
        register(granja, VoucherType.FACTURA_A, OCT_1.minusDays(10), 1, taxed21("1000", "210"));
        register(forrajera, VoucherType.FACTURA_C, OCT_1.plusDays(1), 1, nonTaxed("999"));
        SupplierVoucher invoice = register(granja, VoucherType.FACTURA_A, OCT_1.plusDays(4), 2,
                taxed21("500", "105"));
        SupplierVoucher creditNote = register(granja, VoucherType.NOTA_CREDITO_A, OCT_1.plusDays(6), 1,
                taxed21("100", "21"));
        register(granja, VoucherType.NOTA_DEBITO_A, OCT_31.plusDays(1), 1, taxed21("10", "2.10"));

        SupplierAccount account = reports.supplierAccount(granja.id(), OCT_1, OCT_31);

        assertThat(account.supplier().id()).isEqualTo(granja.id());
        assertThat(account.openingBalance()).isEqualByComparingTo("1210");
        assertThat(account.movements()).hasSize(2);
        AccountMovement first = account.movements().getFirst();
        assertThat(first.voucher().id()).isEqualTo(invoice.id());
        assertThat(first.credit()).isEqualByComparingTo("605");
        assertThat(first.debit()).isEqualByComparingTo("0");
        assertThat(first.balance()).isEqualByComparingTo("1815");
        AccountMovement second = account.movements().getLast();
        assertThat(second.voucher().id()).isEqualTo(creditNote.id());
        assertThat(second.debit()).isEqualByComparingTo("121");
        assertThat(second.credit()).isEqualByComparingTo("0");
        assertThat(second.balance()).isEqualByComparingTo("1694");
        assertThat(account.closingBalance()).isEqualByComparingTo("1694");
    }

    @Test
    void currentAccountWithoutRangeShowsEverything() {
        register(granja, VoucherType.FACTURA_A, OCT_1, 1, taxed21("1000", "210"));
        register(granja, VoucherType.NOTA_DEBITO_A, OCT_31.plusDays(1), 1, taxed21("10", "2.10"));

        SupplierAccount account = reports.supplierAccount(granja.id(), null, null);

        assertThat(account.openingBalance()).isEqualByComparingTo("0");
        assertThat(account.movements()).hasSize(2);
        assertThat(account.closingBalance()).isEqualByComparingTo("1222.10");
    }

    @Test
    void currentAccountOfUnknownSupplierIsNotFound() {
        assertThatThrownBy(() -> reports.supplierAccount(UUID.randomUUID(), OCT_1, OCT_31))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> reports.supplierAccount(granja.id(), OCT_31, OCT_1))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Start date must not be after end date");
    }
}
