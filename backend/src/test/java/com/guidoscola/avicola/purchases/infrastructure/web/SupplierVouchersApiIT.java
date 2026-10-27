package com.guidoscola.avicola.purchases.infrastructure.web;

import com.guidoscola.avicola.purchases.application.SupplierCommand;
import com.guidoscola.avicola.purchases.application.SupplierManagementService;
import com.guidoscola.avicola.purchases.domain.model.Supplier;
import com.guidoscola.avicola.purchases.domain.model.VatCondition;
import com.guidoscola.avicola.support.ApiIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SupplierVouchersApiIT extends ApiIntegrationTest {

    @Autowired
    private SupplierManagementService suppliers;

    private Supplier granja;

    @BeforeEach
    void createSupplier() {
        jdbc.execute("TRUNCATE TABLE suppliers CASCADE");
        granja = suppliers.create(new SupplierCommand("Granja Don Pedro SA", "30-71234567-1",
                VatCondition.RESPONSABLE_INSCRIPTO, null, null, null, null, null));
    }

    private String facturaA(long number, String issueDate) {
        return """
                {"supplierId": "%s", "type": "FACTURA_A", "issueDate": "%s", "dueDate": "2026-11-30",
                 "pointOfSale": 3, "number": %d,
                 "amounts": {"netTaxed21": 1000, "vat21": 210, "vatPerceptions": 30.50}}
                """.formatted(granja.id(), issueDate, number);
    }

    private ResultActions register(String auth, String body) throws Exception {
        return mvc.perform(post("/api/purchases/vouchers").header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int voucherCount() {
        return jdbc.queryForObject("SELECT count(*) FROM supplier_vouchers", Integer.class);
    }

    @Test
    void registeringRequiresPurchasesManage() throws Exception {
        register(bearerWith("PURCHASES_VIEW"), facturaA(1234, "2026-10-05")).andExpect(status().isForbidden());
        assertThat(voucherCount()).isZero();

        register(bearerWith("PURCHASES_MANAGE"), facturaA(1234, "2026-10-05"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.type").value("FACTURA_A"))
                .andExpect(jsonPath("$.pointOfSale").value(3))
                .andExpect(jsonPath("$.number").value(1234))
                .andExpect(jsonPath("$.formattedNumber").value("00003-00001234"))
                .andExpect(jsonPath("$.supplierName").value("Granja Don Pedro SA"))
                .andExpect(jsonPath("$.supplierCuit").value("30-71234567-1"))
                .andExpect(jsonPath("$.amounts.netTaxed21").value(1000.00))
                .andExpect(jsonPath("$.amounts.vat105").value(0.0))
                .andExpect(jsonPath("$.total").value(1240.50));

        assertThat(voucherCount()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidDuplicateAndUnknownTypeVouchers() throws Exception {
        String auth = bearerWith("PURCHASES_MANAGE");
        register(auth, facturaA(1, "2026-10-05")).andExpect(status().isCreated());

        register(auth, facturaA(2, "2026-10-05").replace("\"vat21\": 210", "\"vat21\": 250"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("VAT 21% does not match its net taxed amount"));
        register(auth, facturaA(1, "2026-10-06"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Voucher FACTURA_A 00003-00000001 is already registered for this supplier"));
        register(auth, facturaA(3, "2026-10-05").replace("FACTURA_A", "FACTURA_M"))
                .andExpect(status().isBadRequest());
        register(auth, facturaA(4, "2026-10-05").replace(granja.id().toString(), UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown supplier"));
        register(auth, """
                {"supplierId": "%s", "type": "FACTURA_C", "pointOfSale": 1, "number": 5}
                """.formatted(granja.id()))
                .andExpect(status().isBadRequest());
        register(auth, facturaA(5, "2026-10-05").replace("\"vatPerceptions\": 30.50", "\"exempt\": 1e999999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Amounts must have at most 13 integer digits"));

        assertThat(voucherCount()).isEqualTo(1);
    }

    @Test
    void listingRequiresPurchasesViewAndFiltersByIssueDate() throws Exception {
        String manage = bearerWith("PURCHASES_MANAGE");
        register(manage, facturaA(1, "2026-09-30")).andExpect(status().isCreated());
        String id = com.jayway.jsonpath.JsonPath.read(register(manage, facturaA(2, "2026-10-15"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/purchases/vouchers").param("from", "2026-10-01").param("to", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, bearerWith("SUPPLIERS_VIEW")))
                .andExpect(status().isForbidden());

        String view = bearerWith("PURCHASES_VIEW");
        mvc.perform(get("/api/purchases/vouchers").param("from", "2026-10-01").param("to", "2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, view))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].supplierName").value("Granja Don Pedro SA"));
        mvc.perform(get("/api/purchases/vouchers/{id}", id).header(HttpHeaders.AUTHORIZATION, view))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueDate").value("2026-10-15"))
                .andExpect(jsonPath("$.dueDate").value("2026-11-30"));
        mvc.perform(get("/api/purchases/vouchers/{id}", UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, view))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/purchases/vouchers").param("from", "2026-10-31").param("to", "2026-10-01")
                        .header(HttpHeaders.AUTHORIZATION, view))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Start date must not be after end date"));
    }

    @Test
    void superAdminHoldsTheNewPermissions() throws Exception {
        register(bearer(superAdmin()), facturaA(1, "2026-10-05")).andExpect(status().isCreated());
    }
}
