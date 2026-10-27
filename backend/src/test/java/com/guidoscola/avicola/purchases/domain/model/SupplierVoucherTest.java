package com.guidoscola.avicola.purchases.domain.model;

import com.guidoscola.avicola.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupplierVoucherTest {

    private static final LocalDate ISSUED = LocalDate.of(2026, 10, 5);

    static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    static VoucherAmounts taxed21(String net, String vat) {
        return new VoucherAmounts(money(net), null, null, money(vat), null, null, null, null, null, null, null);
    }

    static VoucherAmounts nonTaxed(String amount) {
        return new VoucherAmounts(null, null, null, null, null, null, money(amount), null, null, null, null);
    }

    private static SupplierVoucher voucher(VoucherType type, VoucherAmounts amounts) {
        return SupplierVoucher.register(UUID.randomUUID(), type, ISSUED, ISSUED.plusDays(30), 1, 1234L, amounts);
    }

    @Test
    void totalAddsEveryConcept() {
        VoucherAmounts amounts = new VoucherAmounts(money("1000"), money("200"), money("100"), money("210"),
                money("21"), money("27"), money("50"), money("40"), money("30"), money("20"), money("10.50"));

        SupplierVoucher voucher = voucher(VoucherType.FACTURA_A, amounts);

        assertThat(voucher.total()).isEqualByComparingTo("1708.50");
        assertThat(voucher.signedTotal()).isEqualByComparingTo("1708.50");
        assertThat(voucher.formattedNumber()).isEqualTo("00001-00001234");
        assertThat(voucher.amounts().netTaxed21()).isEqualTo(money("1000.00"));
        assertThat(voucher.amounts().exempt()).isEqualTo(money("40.00"));
    }

    @Test
    void missingAmountsAreZero() {
        SupplierVoucher voucher = voucher(VoucherType.FACTURA_C, nonTaxed("500"));

        assertThat(voucher.amounts().vat21()).isEqualTo(money("0.00"));
        assertThat(voucher.total()).isEqualByComparingTo("500");
    }

    @Test
    void creditNotesSubtract() {
        SupplierVoucher note = voucher(VoucherType.NOTA_CREDITO_A, taxed21("100", "21"));

        assertThat(note.total()).isEqualByComparingTo("121");
        assertThat(note.signedTotal()).isEqualByComparingTo("-121");
        assertThat(note.signedAmounts().netTaxed21()).isEqualByComparingTo("-100");
        assertThat(note.signedAmounts().vat21()).isEqualByComparingTo("-21");
        assertThat(voucher(VoucherType.NOTA_DEBITO_B, nonTaxed("50")).signedTotal()).isEqualByComparingTo("50");
    }

    @Test
    void onlyLetterADiscriminatesVat() {
        for (VoucherType type : new VoucherType[]{VoucherType.FACTURA_B, VoucherType.FACTURA_C,
                VoucherType.NOTA_CREDITO_B, VoucherType.NOTA_DEBITO_C}) {
            assertThatThrownBy(() -> voucher(type, taxed21("100", "21")))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessage("Only A vouchers discriminate VAT");
        }
    }

    @Test
    void vatMustMatchItsNetTaxedAmountWithinOnePeso() {
        assertThat(voucher(VoucherType.FACTURA_A, taxed21("1000", "211")).total()).isEqualByComparingTo("1211");
        assertThat(voucher(VoucherType.FACTURA_A, new VoucherAmounts(null, money("1000"), money("1000"), null,
                money("105"), money("270"), null, null, null, null, null)).total()).isEqualByComparingTo("2375");

        assertThatThrownBy(() -> voucher(VoucherType.FACTURA_A, taxed21("1000", "212")))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("VAT 21% does not match its net taxed amount");
        assertThatThrownBy(() -> voucher(VoucherType.FACTURA_A, new VoucherAmounts(null, money("1000"), null, null,
                money("210"), null, null, null, null, null, null)))
                .hasMessage("VAT 10.5% does not match its net taxed amount");
        assertThatThrownBy(() -> voucher(VoucherType.FACTURA_A, new VoucherAmounts(null, null, null, null, null,
                money("27"), null, null, null, null, null)))
                .hasMessage("VAT 27% does not match its net taxed amount");
    }

    @Test
    void rejectsNegativeZeroAndOverPreciseAmounts() {
        assertThatThrownBy(() -> voucher(VoucherType.FACTURA_C, nonTaxed("-1")))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Amounts must not be negative");
        assertThatThrownBy(() -> voucher(VoucherType.FACTURA_C, nonTaxed("0")))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Voucher total must be greater than zero");
        assertThatThrownBy(() -> voucher(VoucherType.FACTURA_C, nonTaxed("10.005")))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Amounts must have at most 2 decimals");
    }

    @Test
    void amountsMustFitThirteenIntegerDigits() {
        assertThat(voucher(VoucherType.FACTURA_C, nonTaxed("9999999999999.99")).total())
                .isEqualByComparingTo("9999999999999.99");

        assertThatThrownBy(() -> voucher(VoucherType.FACTURA_C, nonTaxed("10000000000000")))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Amounts must have at most 13 integer digits");
        assertThatThrownBy(() -> voucher(VoucherType.FACTURA_C, nonTaxed("1E+999999999")))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Amounts must have at most 13 integer digits");
    }

    @Test
    void validatesIdentificationAndDates() {
        UUID supplier = UUID.randomUUID();
        VoucherAmounts amounts = nonTaxed("10");

        assertThatThrownBy(() -> SupplierVoucher.register(supplier, null, ISSUED, null, 1, 1L, amounts))
                .hasMessage("Voucher type is required");
        assertThatThrownBy(() -> SupplierVoucher.register(supplier, VoucherType.FACTURA_C, null, null, 1, 1L, amounts))
                .hasMessage("Issue date is required");
        assertThatThrownBy(() -> SupplierVoucher.register(supplier, VoucherType.FACTURA_C, ISSUED, null, 0, 1L, amounts))
                .hasMessage("Point of sale must be between 1 and 99999");
        assertThatThrownBy(() -> SupplierVoucher.register(supplier, VoucherType.FACTURA_C, ISSUED, null, 100000, 1L,
                amounts)).hasMessage("Point of sale must be between 1 and 99999");
        assertThatThrownBy(() -> SupplierVoucher.register(supplier, VoucherType.FACTURA_C, ISSUED, null, 1, 0L, amounts))
                .hasMessage("Number must be between 1 and 99999999");
        assertThatThrownBy(() -> SupplierVoucher.register(supplier, VoucherType.FACTURA_C, ISSUED, null, 1,
                100_000_000L, amounts)).hasMessage("Number must be between 1 and 99999999");
        assertThatThrownBy(() -> SupplierVoucher.register(supplier, VoucherType.FACTURA_C, ISSUED,
                ISSUED.minusDays(1), 1, 1L, amounts)).hasMessage("Due date must not be before the issue date");

        assertThat(SupplierVoucher.register(supplier, VoucherType.FACTURA_C, ISSUED, null, 99999, 99_999_999L,
                amounts).dueDate()).isNull();
    }
}
