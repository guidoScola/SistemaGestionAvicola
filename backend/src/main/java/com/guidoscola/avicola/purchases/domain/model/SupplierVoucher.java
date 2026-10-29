package com.guidoscola.avicola.purchases.domain.model;

import com.guidoscola.avicola.shared.domain.InvalidInputException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A voucher received from a supplier. Amounts are always positive; {@link #signedAmounts()} applies the sign of
 * the voucher type for the VAT ledger and the current account.
 */
public class SupplierVoucher {

    /** Allowed difference between the stated VAT and net × rate, to absorb per-line rounding. */
    private static final BigDecimal VAT_TOLERANCE = new BigDecimal("1.00");

    private final UUID id;
    private final UUID supplierId;
    private final VoucherType type;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final int pointOfSale;
    private final long number;
    private final VoucherAmounts amounts;

    public SupplierVoucher(UUID id, UUID supplierId, VoucherType type, LocalDate issueDate, LocalDate dueDate,
                           int pointOfSale, long number, VoucherAmounts amounts) {
        this.id = Objects.requireNonNull(id);
        this.supplierId = Objects.requireNonNull(supplierId);
        if (type == null) {
            throw new InvalidInputException("Voucher type is required");
        }
        if (issueDate == null) {
            throw new InvalidInputException("Issue date is required");
        }
        if (dueDate != null && dueDate.isBefore(issueDate)) {
            throw new InvalidInputException("Due date must not be before the issue date");
        }
        if (pointOfSale < 1 || pointOfSale > 99_999) {
            throw new InvalidInputException("Point of sale must be between 1 and 99999");
        }
        if (number < 1 || number > 99_999_999L) {
            throw new InvalidInputException("Number must be between 1 and 99999999");
        }
        if (amounts == null) {
            throw new InvalidInputException("Amounts are required");
        }
        validateAmounts(type, amounts);
        this.type = type;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.pointOfSale = pointOfSale;
        this.number = number;
        this.amounts = amounts;
    }

    public static SupplierVoucher register(UUID supplierId, VoucherType type, LocalDate issueDate, LocalDate dueDate,
                                           int pointOfSale, long number, VoucherAmounts amounts) {
        return new SupplierVoucher(UUID.randomUUID(), supplierId, type, issueDate, dueDate, pointOfSale, number,
                amounts);
    }

    private static void validateAmounts(VoucherType type, VoucherAmounts amounts) {
        if (amounts.hasNegative()) {
            throw new InvalidInputException("Amounts must not be negative");
        }
        if (!type.discriminatesVat() && amounts.hasDiscriminatedVat()) {
            throw new InvalidInputException("Only A vouchers discriminate VAT");
        }
        requireVatMatches(amounts.netTaxed21(), amounts.vat21(), "0.21", "21%");
        requireVatMatches(amounts.netTaxed105(), amounts.vat105(), "0.105", "10.5%");
        requireVatMatches(amounts.netTaxed27(), amounts.vat27(), "0.27", "27%");
        if (amounts.total().signum() == 0) {
            throw new InvalidInputException("Voucher total must be greater than zero");
        }
    }

    private static void requireVatMatches(BigDecimal net, BigDecimal vat, String rate, String label) {
        // The rounding tolerance must not let VAT through without any net it could come from.
        if (net.signum() == 0 && vat.signum() != 0) {
            throw new InvalidInputException("VAT " + label + " requires a net taxed amount");
        }
        BigDecimal expected = net.multiply(new BigDecimal(rate));
        if (expected.subtract(vat).abs().compareTo(VAT_TOLERANCE) > 0) {
            throw new InvalidInputException("VAT " + label + " does not match its net taxed amount");
        }
    }

    public BigDecimal total() {
        return amounts.total();
    }

    public VoucherAmounts signedAmounts() {
        return type.isCreditNote() ? amounts.negate() : amounts;
    }

    public BigDecimal signedTotal() {
        return signedAmounts().total();
    }

    /** Point of sale and number as printed on Argentine vouchers, e.g. 00001-00001234. */
    public String formattedNumber() {
        return "%05d-%08d".formatted(pointOfSale, number);
    }

    public UUID id() {
        return id;
    }

    public UUID supplierId() {
        return supplierId;
    }

    public VoucherType type() {
        return type;
    }

    public LocalDate issueDate() {
        return issueDate;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public int pointOfSale() {
        return pointOfSale;
    }

    public long number() {
        return number;
    }

    public VoucherAmounts amounts() {
        return amounts;
    }
}
