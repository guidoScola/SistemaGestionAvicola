package com.guidoscola.avicola.purchases.infrastructure.web;

import com.guidoscola.avicola.purchases.application.SupplierVoucherCommand;
import com.guidoscola.avicola.purchases.application.SupplierVoucherService;
import com.guidoscola.avicola.purchases.application.VoucherEntry;
import com.guidoscola.avicola.purchases.domain.model.Supplier;
import com.guidoscola.avicola.purchases.domain.model.SupplierVoucher;
import com.guidoscola.avicola.purchases.domain.model.VoucherAmounts;
import com.guidoscola.avicola.purchases.domain.model.VoucherType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/purchases/vouchers")
class SupplierVouchersController {

    private final SupplierVoucherService vouchers;

    SupplierVouchersController(SupplierVoucherService vouchers) {
        this.vouchers = vouchers;
    }

    record VoucherResponse(UUID id, UUID supplierId, String supplierName, String supplierCuit, VoucherType type,
                           LocalDate issueDate, LocalDate dueDate, int pointOfSale, long number,
                           String formattedNumber, VoucherAmounts amounts, BigDecimal total) {

        static VoucherResponse from(VoucherEntry entry) {
            SupplierVoucher v = entry.voucher();
            Supplier s = entry.supplier();
            return new VoucherResponse(v.id(), v.supplierId(), s.businessName(), s.cuit().formatted(), v.type(),
                    v.issueDate(), v.dueDate(), v.pointOfSale(), v.number(), v.formattedNumber(), v.amounts(),
                    v.total());
        }
    }

    /** Amounts arrive as plain numbers so domain validation reports a precise 400 instead of a parse error. */
    record AmountsRequest(BigDecimal netTaxed21, BigDecimal netTaxed105, BigDecimal netTaxed27, BigDecimal vat21,
                          BigDecimal vat105, BigDecimal vat27, BigDecimal nonTaxed, BigDecimal exempt,
                          BigDecimal vatPerceptions, BigDecimal grossIncomePerceptions, BigDecimal otherTaxes) {

        VoucherAmounts toAmounts() {
            return new VoucherAmounts(netTaxed21, netTaxed105, netTaxed27, vat21, vat105, vat27, nonTaxed, exempt,
                    vatPerceptions, grossIncomePerceptions, otherTaxes);
        }
    }

    record VoucherRequest(@NotNull UUID supplierId, @NotNull VoucherType type, @NotNull LocalDate issueDate,
                          LocalDate dueDate, @NotNull Integer pointOfSale, @NotNull Long number,
                          @NotNull AmountsRequest amounts) {

        SupplierVoucherCommand toCommand() {
            return new SupplierVoucherCommand(supplierId, type, issueDate, dueDate, pointOfSale, number,
                    amounts.toAmounts());
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PURCHASES_VIEW')")
    List<VoucherResponse> list(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return vouchers.list(from, to).stream().map(VoucherResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASES_VIEW')")
    VoucherResponse get(@PathVariable UUID id) {
        return VoucherResponse.from(vouchers.entry(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PURCHASES_MANAGE')")
    ResponseEntity<VoucherResponse> register(@Valid @RequestBody VoucherRequest request) {
        SupplierVoucher voucher = vouchers.register(request.toCommand());
        return ResponseEntity.created(URI.create("/api/purchases/vouchers/" + voucher.id()))
                .body(VoucherResponse.from(vouchers.entry(voucher.id())));
    }
}
