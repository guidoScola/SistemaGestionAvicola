package com.guidoscola.avicola.security.application;

import com.guidoscola.avicola.security.domain.model.RefreshToken;
import com.guidoscola.avicola.security.domain.model.Role;
import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.shared.domain.ConflictException;
import com.guidoscola.avicola.shared.domain.InvalidInputException;
import com.guidoscola.avicola.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserManagementServiceTest {

    private InMemoryUserRepository users;
    private InMemoryRefreshTokenStore refreshTokens;
    private UserManagementService service;
    private Role seller;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        refreshTokens = new InMemoryRefreshTokenStore();
        InMemoryRoleRepository roles = new InMemoryRoleRepository();
        seller = roles.save(Role.create("Vendedor", Set.of()));
        service = new UserManagementService(users, roles, new FakePasswordHasher(), refreshTokens);
    }

    private void openSessionFor(User user) {
        refreshTokens.save(RefreshToken.issue(user.id(), "hash-" + UUID.randomUUID(), Instant.now().plusSeconds(60)));
    }

    @Test
    void resettingPasswordEndsOpenSessions() {
        User user = createJuan();
        openSessionFor(user);

        service.resetPassword(user.id(), "newSecret1");

        assertThat(refreshTokens.all()).isNotEmpty().allMatch(RefreshToken::revoked);
    }

    @Test
    void changingOwnPasswordEndsOpenSessions() {
        User user = createJuan();
        openSessionFor(user);

        service.changeOwnPassword(user.id(), "secret123", "newSecret1");

        assertThat(refreshTokens.all()).isNotEmpty().allMatch(RefreshToken::revoked);
    }

    @Test
    void deactivatingEndsOpenSessions() {
        User user = createJuan();
        openSessionFor(user);

        service.deactivate(user.id());

        assertThat(refreshTokens.all()).isNotEmpty().allMatch(RefreshToken::revoked);
    }

    @Test
    void failedOwnPasswordChangeKeepsSessions() {
        User user = createJuan();
        openSessionFor(user);

        assertThatThrownBy(() -> service.changeOwnPassword(user.id(), "wrong-pass", "newSecret1"))
                .isInstanceOf(InvalidInputException.class);
        assertThat(refreshTokens.all()).noneMatch(RefreshToken::revoked);
    }

    private User createJuan() {
        return service.create(new CreateUserCommand("jperez", "Juan Perez", "secret123", Set.of(seller.id())));
    }

    @Test
    void createsUserWithHashedPassword() {
        User user = createJuan();

        assertThat(users.findById(user.id())).isPresent();
        assertThat(user.username().value()).isEqualTo("jperez");
        assertThat(user.passwordHash()).isEqualTo("hashed:secret123");
        assertThat(user.roleIds()).containsExactly(seller.id());
    }

    @Test
    void rejectsDuplicatedUsernameIgnoringCase() {
        createJuan();

        assertThatThrownBy(() -> service.create(new CreateUserCommand("JPEREZ", "Otro", "secret123", Set.of())))
                .isInstanceOf(ConflictException.class);
        assertThat(users.findAll()).hasSize(1);
    }

    @Test
    void rejectsPasswordShorterThan8Characters() {
        assertThatThrownBy(() -> service.create(new CreateUserCommand("jperez", "Juan", "short12", Set.of())))
                .isInstanceOf(InvalidInputException.class);
        assertThat(users.findAll()).isEmpty();
    }

    @Test
    void rejectsUnknownRole() {
        assertThatThrownBy(() -> service.create(
                new CreateUserCommand("jperez", "Juan", "secret123", Set.of(UUID.randomUUID()))))
                .isInstanceOf(InvalidInputException.class);
        assertThat(users.findAll()).isEmpty();
    }

    @Test
    void updatesFullNameAndRoles() {
        User user = createJuan();

        User updated = service.update(user.id(), new UpdateUserCommand("Juan P.", Set.of()));

        assertThat(updated.fullName()).isEqualTo("Juan P.");
        assertThat(updated.roleIds()).isEmpty();
    }

    @Test
    void deactivatesAndActivatesUser() {
        User user = createJuan();

        service.deactivate(user.id());
        assertThat(users.findById(user.id()).orElseThrow().isActive()).isFalse();

        service.activate(user.id());
        assertThat(users.findById(user.id()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void adminResetsPassword() {
        User user = createJuan();

        service.resetPassword(user.id(), "newSecret1");

        assertThat(users.findById(user.id()).orElseThrow().passwordHash()).isEqualTo("hashed:newSecret1");
    }

    @Test
    void userChangesOwnPasswordWhenCurrentMatches() {
        User user = createJuan();

        service.changeOwnPassword(user.id(), "secret123", "newSecret1");

        assertThat(users.findById(user.id()).orElseThrow().passwordHash()).isEqualTo("hashed:newSecret1");
    }

    @Test
    void rejectsOwnPasswordChangeWhenCurrentIsWrong() {
        User user = createJuan();

        assertThatThrownBy(() -> service.changeOwnPassword(user.id(), "wrong-pass", "newSecret1"))
                .isInstanceOf(InvalidInputException.class);
        assertThat(users.findById(user.id()).orElseThrow().passwordHash()).isEqualTo("hashed:secret123");
    }

    @Test
    void failsForUnknownUser() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }
}
