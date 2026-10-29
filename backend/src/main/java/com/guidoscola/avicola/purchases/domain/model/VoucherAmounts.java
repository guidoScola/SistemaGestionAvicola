package com.guidoscola.avicola.purchases.domain.model;

import com.guidoscola.avicola.shared.domain.InvalidInputException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

/**
 * The amounts of a voucher by tax concept, the columns of the VAT purchases ledger. Missing amounts are zero and
 * every amount has two decimals. Signs are not checked here: ledger totals and credit notes are negative.
 */
public record VoucherAmounts(BigDecimal netTaxed21, BigDecimal netTaxed105, BigDecimal netTaxed27,
                             BigDecimal vat21, BigDecimal vat105, BigDecimal vat27,
                             BigDecimal nonTaxed, BigDecimal exempt, BigDecimal vatPerceptions,
                             BigDecimal grossIncomePerceptions, BigDecimal otherTaxes) {

    public static final VoucherAmounts ZERO = new VoucherAmounts(null, null, null, null, null, null, null, null, null,
            null, null);

    public VoucherAmounts {
        netTaxed21 = money(netTaxed21);
        netTaxed105 = money(netTaxed105);
        netTaxed27 = money(netTaxed27);
        vat21 = money(vat21);
        vat105 = money(vat105);
        vat27 = money(vat27);
        nonTaxed = money(nonTaxed);
        exempt = money(exempt);
        vatPerceptions = money(vatPerceptions);
        grossIncomePerceptions = money(grossIncomePerceptions);
        otherTaxes = money(otherTaxes);
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new InvalidInputException("Amounts must have at most 2 decimals");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    public BigDecimal total() {
        return all().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean hasNegative() {
        return all().anyMatch(amount -> amount.signum() < 0);
    }

    public boolean hasDiscriminatedVat() {
        return Stream.of(netTaxed21, netTaxed105, netTaxed27, vat21, vat105, vat27)
                .anyMatch(amount -> amount.signum() != 0);
    }

    public VoucherAmounts negate() {
        return new VoucherAmounts(netTaxed21.negate(), netTaxed105.negate(), netTaxed27.negate(), vat21.negate(),
                vat105.negate(), vat27.negate(), nonTaxed.negate(), exempt.negate(), vatPerceptions.negate(),
                grossIncomePerceptions.negate(), otherTaxes.negate());
    }

    public VoucherAmounts plus(VoucherAmounts other) {
        return new VoucherAmounts(netTaxed21.add(other.netTaxed21), netTaxed105.add(other.netTaxed105),
                netTaxed27.add(other.netTaxed27), vat21.add(other.vat21), vat105.add(other.vat105),
                vat27.add(other.vat27), nonTaxed.add(other.nonTaxed), exempt.add(other.exempt),
                vatPerceptions.add(other.vatPerceptions), grossIncomePerceptions.add(other.grossIncomePerceptions),
                otherTaxes.add(other.otherTaxes));
    }

    private Stream<BigDecimal> all() {
        return Stream.of(netTaxed21, netTaxed105, netTaxed27, vat21, vat105, vat27, nonTaxed, exempt, vatPerceptions,
                grossIncomePerceptions, otherTaxes);
    }
}
