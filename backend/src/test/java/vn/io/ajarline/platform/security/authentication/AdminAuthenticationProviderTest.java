package vn.io.ajarline.platform.security.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import vn.io.ajarline.support.AdminAuthenticationTestData;
import vn.io.ajarline.support.MutableClock;

class AdminAuthenticationProviderTest {

    private static final Instant START = Instant.parse("2026-10-29T00:00:00Z");
    private MutableClock clock;
    private AdminAuthenticationProvider provider;

    @BeforeEach
    void prepareAccount() {
        clock = new MutableClock(START, ZoneOffset.UTC);
        provider = new AdminAuthenticationProvider(
                AdminAuthenticationTestData.credentials(),
                Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8(), clock);
    }

    @Test
    void authenticate_ShouldLockAccount_WhenFifthAttemptFails() {
        failFourAttempts();

        assertThatThrownBy(this::wrongPassword)
                .isInstanceOf(AdminLoginLockedException.class)
                .satisfies(failure -> assertThat(
                        ((AdminLoginLockedException) failure).retryAfterSeconds()).isEqualTo(900));
        assertThatThrownBy(this::correctPassword)
                .isInstanceOf(AdminLoginLockedException.class);
    }

    @Test
    void authenticate_ShouldAllowAttempt_WhenLockExpiresExactly() {
        lockAccount();
        clock.set(START.plus(Duration.ofMinutes(15)).minusMillis(1));
        assertThatThrownBy(this::correctPassword)
                .isInstanceOf(AdminLoginLockedException.class);
        clock.set(START.plus(Duration.ofMinutes(15)));

        Authentication result = correctPassword();

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getCredentials()).isNull();
    }

    @Test
    void authenticate_ShouldResetFailures_WhenAuthenticationSucceeds() {
        failFourAttempts();
        correctPassword();

        failFourAttempts();
        assertThat(correctPassword().isAuthenticated()).isTrue();
    }

    @Test
    void authenticate_ShouldResetFailures_WhenExpiredLockGetsAnotherFailure() {
        lockAccount();
        clock.set(START.plus(Duration.ofMinutes(15)));

        failFourAttempts();
        assertThat(correctPassword().isAuthenticated()).isTrue();
    }

    @Test
    void authenticate_ShouldUseSameAccountBudget_WhenUsernameChanges() {
        failFourAttempts();

        assertThatThrownBy(() -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        "unknown-admin", AdminAuthenticationTestData.password())))
                .isInstanceOf(AdminLoginLockedException.class);
    }

    private void failFourAttempts() {
        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(this::wrongPassword)
                    .isExactlyInstanceOf(BadCredentialsException.class);
        }
    }

    private void lockAccount() {
        failFourAttempts();
        assertThatThrownBy(this::wrongPassword)
                .isInstanceOf(AdminLoginLockedException.class);
    }

    private Authentication wrongPassword() {
        return provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                AdminAuthenticationTestData.username(), "invalid"));
    }

    private Authentication correctPassword() {
        return provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                AdminAuthenticationTestData.username(), AdminAuthenticationTestData.password()));
    }
}
