package com.guidoscola.avicola.security.application;

import com.guidoscola.avicola.security.application.port.PasswordHasher;
import com.guidoscola.avicola.security.application.port.RefreshTokenStore;
import com.guidoscola.avicola.security.application.port.RoleRepository;
import com.guidoscola.avicola.security.application.port.UserRepository;
import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.security.domain.model.Username;
import com.guidoscola.avicola.shared.domain.ConflictException;
import com.guidoscola.avicola.shared.domain.InvalidInputException;
import com.guidoscola.avicola.shared.domain.NotFoundException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class UserManagementService {

    static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenStore refreshTokens;

    public UserManagementService(UserRepository users, RoleRepository roles, PasswordHasher passwordHasher,
                                 RefreshTokenStore refreshTokens) {
        this.users = users;
        this.roles = roles;
        this.passwordHasher = passwordHasher;
        this.refreshTokens = refreshTokens;
    }

    public User create(CreateUserCommand command) {
        Username username = new Username(command.username());
        if (users.findByUsername(username).isPresent()) {
            throw new ConflictException("Username already exists: " + username.value());
        }
        Set<UUID> roleIds = requireExistingRoles(command.roleIds());
        String hash = passwordHasher.hash(requireValidPassword(command.password()));
        return users.save(User.create(username, command.fullName(), hash, roleIds));
    }

    public User update(UUID id, UpdateUserCommand command) {
        User user = get(id);
        user.update(command.fullName(), requireExistingRoles(command.roleIds()));
        return users.save(user);
    }

    public void activate(UUID id) {
        User user = get(id);
        user.activate();
        users.save(user);
    }

    public void deactivate(UUID id) {
        User user = get(id);
        user.deactivate();
        users.save(user);
        refreshTokens.revokeAllForUser(id);
    }

    /** Sets a new password and ends every open session of the user. */
    public void resetPassword(UUID id, String newPassword) {
        User user = get(id);
        user.changePasswordHash(passwordHasher.hash(requireValidPassword(newPassword)));
        users.save(user);
        refreshTokens.revokeAllForUser(id);
    }

    public void changeOwnPassword(UUID id, String currentPassword, String newPassword) {
        User user = get(id);
        if (currentPassword == null || !passwordHasher.matches(currentPassword, user.passwordHash())) {
            throw new InvalidInputException("Current password is incorrect");
        }
        user.changePasswordHash(passwordHasher.hash(requireValidPassword(newPassword)));
        users.save(user);
        refreshTokens.revokeAllForUser(id);
    }

    public User get(UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    public List<User> list() {
        return users.findAll();
    }

    private Set<UUID> requireExistingRoles(Set<UUID> roleIds) {
        if (roleIds != null && roleIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new InvalidInputException("Role ids must not be null");
        }
        Set<UUID> ids = roleIds == null ? Set.of() : Set.copyOf(roleIds);
        if (roles.findAllById(ids).size() != ids.size()) {
            throw new InvalidInputException("One or more roles do not exist");
        }
        return ids;
    }

    private static String requireValidPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidInputException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        return password;
    }
}
