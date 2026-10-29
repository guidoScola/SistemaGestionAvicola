package com.guidoscola.avicola.purchases.infrastructure.web;

import com.guidoscola.avicola.purchases.application.SupplierCommand;
import com.guidoscola.avicola.purchases.application.SupplierManagementService;
import com.guidoscola.avicola.purchases.domain.model.Supplier;
import com.guidoscola.avicola.purchases.domain.model.VatCondition;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers")
class SuppliersController {

    private final SupplierManagementService suppliers;

    SuppliersController(SupplierManagementService suppliers) {
        this.suppliers = suppliers;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SupplierResponse(UUID id, String businessName, String cuit, VatCondition vatCondition, String address,
                            String city, String province, String phone, String email, boolean active) {

        static SupplierResponse from(Supplier s) {
            return new SupplierResponse(s.id(), s.businessName(), s.cuit().formatted(), s.vatCondition(), s.address(),
                    s.city(), s.province(), s.phone(), s.email(), s.isActive());
        }
    }

    record SupplierRequest(@NotBlank String businessName, @NotBlank String cuit, @NotNull VatCondition vatCondition,
                           String address, String city, String province, String phone, String email) {

        SupplierCommand toCommand() {
            return new SupplierCommand(businessName, cuit, vatCondition, address, city, province, phone, email);
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIERS_VIEW')")
    List<SupplierResponse> list() {
        return suppliers.list().stream().map(SupplierResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIERS_VIEW')")
    SupplierResponse get(@PathVariable UUID id) {
        return SupplierResponse.from(suppliers.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIERS_MANAGE')")
    ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        Supplier supplier = suppliers.create(request.toCommand());
        return ResponseEntity.created(URI.create("/api/suppliers/" + supplier.id()))
                .body(SupplierResponse.from(supplier));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIERS_MANAGE')")
    SupplierResponse update(@PathVariable UUID id, @Valid @RequestBody SupplierRequest request) {
        return SupplierResponse.from(suppliers.update(id, request.toCommand()));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SUPPLIERS_MANAGE')")
    ResponseEntity<Void> activate(@PathVariable UUID id) {
        suppliers.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('SUPPLIERS_MANAGE')")
    ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        suppliers.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
