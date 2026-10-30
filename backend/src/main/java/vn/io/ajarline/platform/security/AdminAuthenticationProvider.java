package vn.io.ajarline.platform.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class AdminAuthenticationProvider implements AuthenticationProvider {

    private static final int FAILURE_LIMIT = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final AdminCredentials credentials;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private int failureAttempts;
    private Instant lockedUntil;

    public AdminAuthenticationProvider(AdminCredentials credentials, PasswordEncoder encoder, Clock clock) {
        this.credentials = credentials;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    public synchronized Authentication authenticate(Authentication authentication){
        Instant now = clock.instant();
        if (lockedUntil != null) {
            if (now.isBefore(lockedUntil)) {
                throw locked(now);
            }
            failureAttempts = 0;
            lockedUntil = null;
        }

        String password = authentication.getCredentials() instanceof String value ? value : "";

        boolean isPasswordMatch = encoder.matches(password, credentials.passwordHash());
        boolean isUsernameMatch = credentials.username().equals(authentication.getName());
        if (isUsernameMatch && isPasswordMatch) {
            failureAttempts = 0;
            return UsernamePasswordAuthenticationToken.authenticated(
                    credentials.username(),
                    null,
                    AuthorityUtils.createAuthorityList("ROLE_ADMIN")
            );
        }

        failureAttempts ++;
        if (failureAttempts >= FAILURE_LIMIT) {
            now = clock.instant();
            lockedUntil = now.plus(LOCK_DURATION);
            throw locked(now);
        }
        throw new BadCredentialsException("Authentication failed.");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * Creates an exception with the remaining account lock time in seconds.
     * Rounds the time up to at least one second.
     *
     * @param now the current time
     * @return an exception containing the remaining lock time
     */
    private AdminLoginLockedException locked(Instant now) {
        Duration remaining = Duration.between(now, lockedUntil);
        long seconds = remaining.toSeconds();
        if (remaining.getNano() != 0) {
            seconds ++;
        }
        return new AdminLoginLockedException(Math.max(1, seconds));
    }
}
