package com.guidoscola.avicola.purchases.infrastructure.web;

import com.guidoscola.avicola.purchases.application.SupplierCommand;
import com.guidoscola.avicola.purchases.application.SupplierManagementService;
import com.guidoscola.avicola.purchases.application.SupplierVoucherCommand;
import com.guidoscola.avicola.purchases.application.SupplierVoucherService;
import com.guidoscola.avicola.purchases.domain.model.Supplier;
import com.guidoscola.avicola.purchases.domain.model.VatCondition;
import com.guidoscola.avicola.purchases.domain.model.VoucherAmounts;
import com.guidoscola.avicola.purchases.domain.model.VoucherType;
import com.guidoscola.avicola.support.ApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PurchaseReportsApiIT extends ApiIntegrationTest {

    @Autowired
    private SupplierManagementService suppliers;

    @Autowired
    private SupplierVoucherService vouchers;

    private Supplier granja;

    @BeforeEach
    void createVouchers() {
        jdbc.execute("TRUNCATE TABLE suppliers CASCADE");
        granja = suppliers.create(new SupplierCommand("Granja Don Pedro SA", "30-71234567-1",
                VatCondition.RESPONSABLE_INSCRIPTO, null, null, null, null, null));
        register(VoucherType.FACTURA_A, "2026-09-20", 1, "1000", "210");
        register(VoucherType.FACTURA_A, "2026-10-05", 2, "500", "105");
        register(VoucherType.NOTA_CREDITO_A, "2026-10-07", 1, "100", "21");
    }

    private void register(VoucherType type, String issued, long number, String net, String vat) {
        vouchers.register(new SupplierVoucherCommand(granja.id(), type, LocalDate.parse(issued), null, 1, number,
                new VoucherAmounts(new BigDecimal(net), null, null, new BigDecimal(vat), null, null, null, null, null,
                        null, null)));
    }

    @Test
    void vatLedgerRequiresPurchasesView() throws Exception {
        mvc.perform(get("/api/purchases/vat-ledger").param("from", "2026-10-01").param("to", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, bearerWith("SUPPLIERS_VIEW")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/purchases/vat-ledger").param("from", "2026-10-01").param("to", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, bearerWith("PURCHASES_VIEW")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-10-01"))
                .andExpect(jsonPath("$.rows.length()").value(2))
                .andExpect(jsonPath("$.rows[0].issueDate").value("2026-10-05"))
                .andExpect(jsonPath("$.rows[0].type").value("FACTURA_A"))
                .andExpect(jsonPath("$.rows[0].formattedNumber").value("00001-00000002"))
                .andExpect(jsonPath("$.rows[0].supplierName").value("Granja Don Pedro SA"))
                .andExpect(jsonPath("$.rows[0].supplierCuit").value("30-71234567-1"))
                .andExpect(jsonPath("$.rows[0].vatCondition").value("RESPONSABLE_INSCRIPTO"))
                .andExpect(jsonPath("$.rows[1].amounts.netTaxed21").value(-100.0))
                .andExpect(jsonPath("$.rows[1].total").value(-121.0))
                .andExpect(jsonPath("$.totals.netTaxed21").value(400.0))
                .andExpect(jsonPath("$.totals.vat21").value(84.0))
                .andExpect(jsonPath("$.total").value(484.0));
    }

    @Test
    void vatLedgerRejectsMissingOrInvertedRange() throws Exception {
        String auth = bearerWith("PURCHASES_VIEW");

        mvc.perform(get("/api/purchases/vat-ledger").param("from", "2026-10-01")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/purchases/vat-ledger").param("from", "2026-10-31").param("to", "2026-10-01")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Start date must not be after end date"));
    }

    @Test
    void supplierAccountShowsBalances() throws Exception {
        mvc.perform(get("/api/purchases/suppliers/{id}/account", granja.id()).param("from", "2026-10-01")
                        .param("to", "2026-10-31").header(HttpHeaders.AUTHORIZATION, bearerWith("SUPPLIERS_VIEW")))
                .andExpect(status().isForbidden());

        String auth = bearerWith("PURCHASES_VIEW");
        mvc.perform(get("/api/purchases/suppliers/{id}/account", granja.id()).param("from", "2026-10-01")
                        .param("to", "2026-10-31").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierId").value(granja.id().toString()))
                .andExpect(jsonPath("$.supplierName").value("Granja Don Pedro SA"))
                .andExpect(jsonPath("$.openingBalance").value(1210.0))
                .andExpect(jsonPath("$.movements.length()").value(2))
                .andExpect(jsonPath("$.movements[0].type").value("FACTURA_A"))
                .andExpect(jsonPath("$.movements[0].formattedNumber").value("00001-00000002"))
                .andExpect(jsonPath("$.movements[0].credit").value(605.0))
                .andExpect(jsonPath("$.movements[0].debit").value(0.0))
                .andExpect(jsonPath("$.movements[0].balance").value(1815.0))
                .andExpect(jsonPath("$.movements[1].debit").value(121.0))
                .andExpect(jsonPath("$.movements[1].balance").value(1694.0))
                .andExpect(jsonPath("$.closingBalance").value(1694.0));

        mvc.perform(get("/api/purchases/suppliers/{id}/account", granja.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBalance").value(0.0))
                .andExpect(jsonPath("$.movements.length()").value(3))
                .andExpect(jsonPath("$.closingBalance").value(1694.0));
        mvc.perform(get("/api/purchases/suppliers/{id}/account", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }
}
