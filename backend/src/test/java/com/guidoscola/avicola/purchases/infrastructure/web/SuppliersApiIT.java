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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SuppliersApiIT extends ApiIntegrationTest {

    private static final String GRANJA = """
            {"businessName": "Granja Don Pedro SA", "cuit": "30-71234567-1", "vatCondition": "RESPONSABLE_INSCRIPTO",
             "address": "Ruta 2 km 40", "city": "Chascomús", "province": "Buenos Aires",
             "phone": "02241-123456", "email": "ventas@donpedro.com.ar"}
            """;

    @Autowired
    private SupplierManagementService suppliers;

    @BeforeEach
    void cleanSuppliers() {
        jdbc.execute("TRUNCATE TABLE suppliers CASCADE");
    }

    private Supplier existing() {
        return suppliers.create(new SupplierCommand("Forrajera Sur", "20-12345678-6", VatCondition.MONOTRIBUTISTA,
                null, "Dolores", "Buenos Aires", null, null));
    }

    @Test
    void listingRequiresSuppliersView() throws Exception {
        existing();

        mvc.perform(get("/api/suppliers")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/suppliers").header(HttpHeaders.AUTHORIZATION, bearerWith("USERS_VIEW")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/suppliers").header(HttpHeaders.AUTHORIZATION, bearerWith("SUPPLIERS_VIEW")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].businessName", hasItem("Forrajera Sur")))
                .andExpect(jsonPath("$[0].cuit").value("20-12345678-6"))
                .andExpect(jsonPath("$[0].vatCondition").value("MONOTRIBUTISTA"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void getsOneSupplierOr404() throws Exception {
        Supplier supplier = existing();
        String auth = bearerWith("SUPPLIERS_VIEW");

        mvc.perform(get("/api/suppliers/{id}", supplier.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Dolores"));
        mvc.perform(get("/api/suppliers/{id}", UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingRequiresSuppliersManage() throws Exception {
        mvc.perform(post("/api/suppliers").header(HttpHeaders.AUTHORIZATION, bearerWith("SUPPLIERS_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON).content(GRANJA))
                .andExpect(status().isForbidden());
        assertThat(suppliers.list()).isEmpty();

        mvc.perform(post("/api/suppliers").header(HttpHeaders.AUTHORIZATION, bearerWith("SUPPLIERS_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON).content(GRANJA))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.cuit").value("30-71234567-1"))
                .andExpect(jsonPath("$.email").value("ventas@donpedro.com.ar"));

        assertThat(suppliers.list()).extracting(Supplier::businessName).containsExactly("Granja Don Pedro SA");
    }

    @Test
    void rejectsInvalidCuitDuplicatesAndUnknownVatCondition() throws Exception {
        existing();
        String auth = bearerWith("SUPPLIERS_MANAGE");

        mvc.perform(post("/api/suppliers").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GRANJA.replace("30-71234567-1", "30-71234567-2")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid CUIT"));
        mvc.perform(post("/api/suppliers").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GRANJA.replace("30-71234567-1", "20123456786")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/suppliers").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GRANJA.replace("RESPONSABLE_INSCRIPTO", "CUALQUIERA")))
                .andExpect(status().isBadRequest());

        assertThat(suppliers.list()).hasSize(1);
    }

    @Test
    void updatesSupplier() throws Exception {
        Supplier supplier = existing();

        mvc.perform(put("/api/suppliers/{id}", supplier.id())
                        .header(HttpHeaders.AUTHORIZATION, bearerWith("SUPPLIERS_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"businessName": "Forrajera Sur SRL", "cuit": "20-12345678-6",
                                 "vatCondition": "RESPONSABLE_INSCRIPTO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Forrajera Sur SRL"))
                .andExpect(jsonPath("$.vatCondition").value("RESPONSABLE_INSCRIPTO"))
                .andExpect(jsonPath("$.city").doesNotExist());
    }

    @Test
    void deactivatesAndReactivates() throws Exception {
        Supplier supplier = existing();
        String auth = bearerWith("SUPPLIERS_MANAGE");

        mvc.perform(post("/api/suppliers/{id}/deactivate", supplier.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
        assertThat(suppliers.get(supplier.id()).isActive()).isFalse();

        mvc.perform(post("/api/suppliers/{id}/activate", supplier.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
        assertThat(suppliers.get(supplier.id()).isActive()).isTrue();
    }

    @Test
    void superAdminHoldsTheNewPermissions() throws Exception {
        existing();

        mvc.perform(get("/api/suppliers").header(HttpHeaders.AUTHORIZATION, bearer(superAdmin())))
                .andExpect(status().isOk());
    }
}
