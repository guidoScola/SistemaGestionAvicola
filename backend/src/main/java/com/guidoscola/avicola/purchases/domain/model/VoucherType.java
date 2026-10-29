package com.guidoscola.avicola.purchases.domain.model;

/**
 * Supplier voucher types. Credit notes reduce purchases and the debt with the supplier; invoices and debit notes
 * increase them.
 */
public enum VoucherType {
    FACTURA_A('A', 1),
    FACTURA_B('B', 1),
    FACTURA_C('C', 1),
    NOTA_DEBITO_A('A', 1),
    NOTA_DEBITO_B('B', 1),
    NOTA_DEBITO_C('C', 1),
    NOTA_CREDITO_A('A', -1),
    NOTA_CREDITO_B('B', -1),
    NOTA_CREDITO_C('C', -1);

    private final char letter;
    private final int sign;

    VoucherType(char letter, int sign) {
        this.letter = letter;
        this.sign = sign;
    }

    /** Only A vouchers show VAT separately, so only they carry net taxed amounts and VAT. */
    public boolean discriminatesVat() {
        return letter == 'A';
    }

    public boolean isCreditNote() {
        return sign < 0;
    }

    public int sign() {
        return sign;
    }
}
