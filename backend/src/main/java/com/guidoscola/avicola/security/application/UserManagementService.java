package com.guidoscola.avicola.security.application;

import com.guidoscola.avicola.security.application.port.PasswordHasher;
import com.guidoscola.avicola.security.application.port.RefreshTokenStore;
import com.guidoscola.avicola.security.application.port.RoleRepository;
import com.guidoscola.avicola.security.application.port.UserRepository;
import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.security.domain.model.Username;
import com.guidoscola.avicola.shared.domain.ConflictException;
import com.guidoscola.avicola.shared.domain.ForbiddenException;
import com.guidoscola.avicola.shared.domain.InvalidInputException;
import com.guidoscola.avicola.shared.domain.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * User administration. Every mutating operation receives the acting user ({@code actorId}) and enforces:
 * <ul>
 *     <li>only a super admin can manage a super admin;</li>
 *     <li>a non super admin can only assign roles whose permissions they already hold, and cannot reset the
 *     password of someone holding permissions they lack;</li>
 *     <li>the last active super admin cannot be deactivated.</li>
 * </ul>
 */
public class UserManagementService {

    static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenStore refreshTokens;
    private final PermissionResolver permissions;

    public UserManagementService(UserRepository users, RoleRepository roles, PasswordHasher passwordHasher,
                                 RefreshTokenStore refreshTokens, PermissionResolver permissions) {
        this.users = users;
        this.roles = roles;
        this.passwordHasher = passwordHasher;
        this.refreshTokens = refreshTokens;
        this.permissions = permissions;
    }

    /** Creates the top-level administrator. Only meant for system bootstrap. */
    public User createSuperAdmin(String username, String fullName, String password) {
        Username name = requireFreeUsername(username);
        String hash = passwordHasher.hash(requireValidPassword(password));
        return users.save(User.createSuperAdmin(name, fullName, hash));
    }

    public User create(UUID actorId, CreateUserCommand command) {
        User actor = actor(actorId);
        Username username = requireFreeUsername(command.username());
        Set<UUID> roleIds = requireExistingRoles(command.roleIds());
        requireCanAssign(actor, roleIds, Set.of());
        String hash = passwordHasher.hash(requireValidPassword(command.password()));
        return users.save(User.create(username, command.fullName(), hash, roleIds));
    }

    public User update(UUID actorId, UUID id, UpdateUserCommand command) {
        User actor = actor(actorId);
        User user = get(id);
        requireCanManage(actor, user);
        Set<UUID> roleIds = requireExistingRoles(command.roleIds());
        requireCanAssign(actor, roleIds, user.roleIds());
        user.update(command.fullName(), roleIds);
        return users.save(user);
    }

    public void activate(UUID actorId, UUID id) {
        User user = get(id);
        requireCanManage(actor(actorId), user);
        user.activate();
        users.save(user);
    }

    public void deactivate(UUID actorId, UUID id) {
        User user = get(id);
        requireCanManage(actor(actorId), user);
        if (user.isSuperAdmin() && user.isActive() && activeSuperAdmins() == 1) {
            throw new ConflictException("The last active super administrator cannot be deactivated");
        }
        user.deactivate();
        users.save(user);
        refreshTokens.revokeAllForUser(id);
    }

    /** Sets a new password and ends every open session of the user. */
    public void resetPassword(UUID actorId, UUID id, String newPassword) {
        User actor = actor(actorId);
        User user = get(id);
        requireCanManage(actor, user);
        if (!actor.isSuperAdmin()
                && !permissions.effectivePermissions(actor).containsAll(permissions.effectivePermissions(user))) {
            throw new ForbiddenException("You cannot reset the password of a user with permissions you do not hold");
        }
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

    private User actor(UUID actorId) {
        return users.findById(actorId)
                .filter(User::isActive)
                .orElseThrow(() -> new ForbiddenException("Unknown or inactive acting user"));
    }

    private static void requireCanManage(User actor, User target) {
        if (target.isSuperAdmin() && !actor.isSuperAdmin()) {
            throw new ForbiddenException("Only a super administrator can manage a super administrator");
        }
    }

    /** Roles the target already had are not an escalation; only newly granted roles are checked. */
    private void requireCanAssign(User actor, Set<UUID> requestedRoleIds, Set<UUID> currentRoleIds) {
        if (actor.isSuperAdmin()) {
            return;
        }
        Set<UUID> added = new HashSet<>(requestedRoleIds);
        added.removeAll(currentRoleIds);
        if (!permissions.effectivePermissions(actor).containsAll(permissions.permissionsOfRoles(added))) {
            throw new ForbiddenException("You can only assign roles whose permissions you already hold");
        }
    }

    private long activeSuperAdmins() {
        return users.findAll().stream().filter(u -> u.isSuperAdmin() && u.isActive()).count();
    }

    private Username requireFreeUsername(String raw) {
        Username username = new Username(raw);
        if (users.findByUsername(username).isPresent()) {
            throw new ConflictException("Username already exists: " + username.value());
        }
        return username;
    }

    private Set<UUID> requireExistingRoles(Set<UUID> roleIds) {
        if (roleIds != null && roleIds.stream().anyMatch(Objects::isNull)) {
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
