package com.guidoscola.avicola.security.infrastructure.web;

import com.guidoscola.avicola.security.application.CreateUserCommand;
import com.guidoscola.avicola.security.application.UpdateUserCommand;
import com.guidoscola.avicola.security.application.UserManagementService;
import com.guidoscola.avicola.security.domain.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
class UsersController {

    private final UserManagementService users;

    UsersController(UserManagementService users) {
        this.users = users;
    }

    record UserResponse(UUID id, String username, String fullName, boolean active, Set<UUID> roleIds) {

        static UserResponse from(User user) {
            return new UserResponse(user.id(), user.username().value(), user.fullName(), user.isActive(),
                    user.roleIds());
        }
    }

    record CreateUserRequest(@NotBlank String username, @NotBlank String fullName, @NotBlank String password,
                             @NotNull Set<UUID> roleIds) {
    }

    record UpdateUserRequest(@NotBlank String fullName, @NotNull Set<UUID> roleIds) {
    }

    record ResetPasswordRequest(@NotBlank String newPassword) {
    }

    record ChangeOwnPasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USERS_VIEW')")
    List<UserResponse> list() {
        return users.list().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USERS_VIEW')")
    UserResponse get(@PathVariable UUID id) {
        return UserResponse.from(users.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USERS_MANAGE')")
    ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User user = users.create(new CreateUserCommand(request.username(), request.fullName(), request.password(),
                request.roleIds()));
        return ResponseEntity.created(URI.create("/api/users/" + user.id())).body(UserResponse.from(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USERS_MANAGE')")
    UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(users.update(id, new UpdateUserCommand(request.fullName(), request.roleIds())));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('USERS_MANAGE')")
    ResponseEntity<Void> activate(@PathVariable UUID id) {
        users.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('USERS_MANAGE')")
    ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        users.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('USERS_MANAGE')")
    ResponseEntity<Void> resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        users.resetPassword(id, request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/password")
    ResponseEntity<Void> changeOwnPassword(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody ChangeOwnPasswordRequest request) {
        users.changeOwnPassword(UUID.fromString(jwt.getSubject()), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
