package com.guidoscola.avicola.security.application;

import com.guidoscola.avicola.security.application.port.AccessTokenIssuer;
import com.guidoscola.avicola.security.domain.model.PermissionCode;
import com.guidoscola.avicola.security.domain.model.RefreshToken;
import com.guidoscola.avicola.security.domain.model.Role;
import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.security.domain.model.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationServiceTest {

    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    private InMemoryUserRepository users;
    private InMemoryRefreshTokenStore refreshTokens;
    private MutableClock clock;
    private AuthenticationService service;
    private User juan;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        InMemoryRoleRepository roles = new InMemoryRoleRepository();
        refreshTokens = new InMemoryRefreshTokenStore();
        clock = new MutableClock(Instant.parse("2026-10-26T12:00:00Z"));
        FakePasswordHasher hasher = new FakePasswordHasher();
        AccessTokenIssuer accessTokens = (user, permissions) ->
                new AccessToken("access-for-" + user.username().value(), clock.instant().plusSeconds(900));

        Role seller = roles.save(Role.create("Vendedor", Set.of(new PermissionCode("USERS_VIEW"))));
        Role auditor = roles.save(Role.create("Auditor", Set.of(new PermissionCode("ROLES_VIEW"))));
        juan = users.save(User.create(new Username("jperez"), "Juan Perez", hasher.hash("secret123"),
                Set.of(seller.id(), auditor.id())));

        service = new AuthenticationService(users, roles, hasher, refreshTokens, accessTokens, clock, REFRESH_TTL);
    }

    @Test
    void loginReturnsTokensAndUnionOfRolePermissions() {
        AuthSession session = service.login("JPerez", "secret123");

        assertThat(session.accessToken().value()).isEqualTo("access-for-jperez");
        assertThat(session.refreshToken()).isNotBlank();
        assertThat(session.refreshExpiresAt()).isEqualTo(clock.instant().plus(REFRESH_TTL));
        assertThat(session.user().username()).isEqualTo("jperez");
        assertThat(session.user().fullName()).isEqualTo("Juan Perez");
        assertThat(session.user().permissions()).containsExactlyInAnyOrder("USERS_VIEW", "ROLES_VIEW");
    }

    @Test
    void storesOnlyTheHashOfTheRefreshToken() {
        AuthSession session = service.login("jperez", "secret123");

        assertThat(refreshTokens.all()).hasSize(1);
        assertThat(refreshTokens.all().getFirst().tokenHash()).isNotEqualTo(session.refreshToken());
    }

    @Test
    void rejectsWrongPassword() {
        assertThatThrownBy(() -> service.login("jperez", "wrong-pass"))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(refreshTokens.all()).isEmpty();
    }

    @Test
    void rejectsUnknownOrMalformedUsername() {
        assertThatThrownBy(() -> service.login("nobody", "secret123"))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> service.login("a b", "secret123"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void rejectsInactiveUser() {
        juan.deactivate();
        users.save(juan);

        assertThatThrownBy(() -> service.login("jperez", "secret123"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void refreshRotatesTheToken() {
        AuthSession first = service.login("jperez", "secret123");

        AuthSession second = service.refresh(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(second.user().permissions()).containsExactlyInAnyOrder("USERS_VIEW", "ROLES_VIEW");
        assertThatThrownBy(() -> service.refresh(first.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void reusingARotatedTokenRevokesEverySessionOfTheUser() {
        AuthSession first = service.login("jperez", "secret123");
        AuthSession second = service.refresh(first.refreshToken());

        assertThatThrownBy(() -> service.refresh(first.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> service.refresh(second.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void losingAConcurrentRefreshRevokesEverySessionOfTheUser() {
        AuthSession session = service.login("jperez", "secret123");
        InMemoryRefreshTokenStore racingStore = new InMemoryRefreshTokenStore() {
            @Override
            public boolean revoke(java.util.UUID tokenId) {
                return false;
            }
        };
        refreshTokens.all().forEach(racingStore::save);
        AuthenticationService racingService = new AuthenticationService(users, new InMemoryRoleRepository(),
                new FakePasswordHasher(), racingStore, (u, p) -> new AccessToken("x", clock.instant()), clock,
                REFRESH_TTL);

        assertThatThrownBy(() -> racingService.refresh(session.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(racingStore.all()).allMatch(RefreshToken::revoked);
    }

    @Test
    void rejectsExpiredRefreshToken() {
        AuthSession session = service.login("jperez", "secret123");
        clock.advance(REFRESH_TTL.plusSeconds(1));

        assertThatThrownBy(() -> service.refresh(session.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void rejectsRefreshForDeactivatedUser() {
        AuthSession session = service.login("jperez", "secret123");
        juan.deactivate();
        users.save(juan);

        assertThatThrownBy(() -> service.refresh(session.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void rejectsUnknownRefreshToken() {
        assertThatThrownBy(() -> service.refresh("does-not-exist"))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> service.refresh(null))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void logoutRevokesTheRefreshToken() {
        AuthSession session = service.login("jperez", "secret123");

        service.logout(session.refreshToken());

        assertThat(refreshTokens.all()).allMatch(RefreshToken::revoked);
        assertThatThrownBy(() -> service.refresh(session.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void logoutWithUnknownTokenDoesNothing() {
        service.logout("does-not-exist");
        service.logout(null);

        assertThat(refreshTokens.all()).isEmpty();
    }

    static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
