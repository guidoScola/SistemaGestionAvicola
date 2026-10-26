package com.guidoscola.avicola.security.infrastructure.web;

import com.guidoscola.avicola.security.application.RoleCommand;
import com.guidoscola.avicola.security.application.RoleManagementService;
import com.guidoscola.avicola.security.domain.model.PermissionCode;
import com.guidoscola.avicola.security.domain.model.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
class RolesController {

    private final RoleManagementService roles;

    RolesController(RoleManagementService roles) {
        this.roles = roles;
    }

    record RoleResponse(UUID id, String name, List<String> permissionCodes) {

        static RoleResponse from(Role role) {
            return new RoleResponse(role.id(), role.name(),
                    role.permissions().stream().map(PermissionCode::value).sorted().toList());
        }
    }

    record RoleRequest(@NotBlank String name, @NotNull Set<String> permissionCodes) {
    }

    record PermissionResponse(String code, String description) {
    }

    @GetMapping("/api/roles")
    @PreAuthorize("hasAuthority('ROLES_VIEW')")
    List<RoleResponse> list() {
        return roles.list().stream().map(RoleResponse::from).toList();
    }

    @GetMapping("/api/roles/{id}")
    @PreAuthorize("hasAuthority('ROLES_VIEW')")
    RoleResponse get(@PathVariable UUID id) {
        return RoleResponse.from(roles.get(id));
    }

    @PostMapping("/api/roles")
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
        Role role = roles.create(new RoleCommand(request.name(), request.permissionCodes()));
        return ResponseEntity.created(URI.create("/api/roles/" + role.id())).body(RoleResponse.from(role));
    }

    @PutMapping("/api/roles/{id}")
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    RoleResponse update(@PathVariable UUID id, @Valid @RequestBody RoleRequest request) {
        return RoleResponse.from(roles.update(id, new RoleCommand(request.name(), request.permissionCodes())));
    }

    @DeleteMapping("/api/roles/{id}")
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        roles.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/permissions")
    @PreAuthorize("hasAuthority('ROLES_VIEW')")
    List<PermissionResponse> permissions() {
        return roles.permissionCatalog().stream()
                .map(p -> new PermissionResponse(p.code().value(), p.description()))
                .toList();
    }
}
