package com.guidoscola.avicola.purchases.infrastructure.persistence;

import com.guidoscola.avicola.purchases.domain.model.SupplierVoucher;
import com.guidoscola.avicola.purchases.domain.model.VoucherAmounts;
import com.guidoscola.avicola.purchases.domain.model.VoucherType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "supplier_vouchers")
class SupplierVoucherEntity {

    @Id
    private UUID id;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoucherType type;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "point_of_sale", nullable = false)
    private int pointOfSale;

    @Column(nullable = false)
    private long number;

    @Column(name = "net_taxed_21", nullable = false, precision = 15, scale = 2)
    private BigDecimal netTaxed21;

    @Column(name = "net_taxed_105", nullable = false, precision = 15, scale = 2)
    private BigDecimal netTaxed105;

    @Column(name = "net_taxed_27", nullable = false, precision = 15, scale = 2)
    private BigDecimal netTaxed27;

    @Column(name = "vat_21", nullable = false, precision = 15, scale = 2)
    private BigDecimal vat21;

    @Column(name = "vat_105", nullable = false, precision = 15, scale = 2)
    private BigDecimal vat105;

    @Column(name = "vat_27", nullable = false, precision = 15, scale = 2)
    private BigDecimal vat27;

    @Column(name = "non_taxed", nullable = false, precision = 15, scale = 2)
    private BigDecimal nonTaxed;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal exempt;

    @Column(name = "vat_perceptions", nullable = false, precision = 15, scale = 2)
    private BigDecimal vatPerceptions;

    @Column(name = "gross_income_perceptions", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossIncomePerceptions;

    @Column(name = "other_taxes", nullable = false, precision = 15, scale = 2)
    private BigDecimal otherTaxes;

    protected SupplierVoucherEntity() {
    }

    static SupplierVoucherEntity fromDomain(SupplierVoucher voucher) {
        SupplierVoucherEntity entity = new SupplierVoucherEntity();
        VoucherAmounts amounts = voucher.amounts();
        entity.id = voucher.id();
        entity.supplierId = voucher.supplierId();
        entity.type = voucher.type();
        entity.issueDate = voucher.issueDate();
        entity.dueDate = voucher.dueDate();
        entity.pointOfSale = voucher.pointOfSale();
        entity.number = voucher.number();
        entity.netTaxed21 = amounts.netTaxed21();
        entity.netTaxed105 = amounts.netTaxed105();
        entity.netTaxed27 = amounts.netTaxed27();
        entity.vat21 = amounts.vat21();
        entity.vat105 = amounts.vat105();
        entity.vat27 = amounts.vat27();
        entity.nonTaxed = amounts.nonTaxed();
        entity.exempt = amounts.exempt();
        entity.vatPerceptions = amounts.vatPerceptions();
        entity.grossIncomePerceptions = amounts.grossIncomePerceptions();
        entity.otherTaxes = amounts.otherTaxes();
        return entity;
    }

    SupplierVoucher toDomain() {
        return new SupplierVoucher(id, supplierId, type, issueDate, dueDate, pointOfSale, number,
                new VoucherAmounts(netTaxed21, netTaxed105, netTaxed27, vat21, vat105, vat27, nonTaxed, exempt,
                        vatPerceptions, grossIncomePerceptions, otherTaxes));
    }
}
