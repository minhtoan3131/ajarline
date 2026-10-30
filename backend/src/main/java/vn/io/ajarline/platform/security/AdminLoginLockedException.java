package vn.io.ajarline.platform.security;

import org.springframework.security.core.AuthenticationException;

public class AdminLoginLockedException extends AuthenticationException {
    private final long retryAfterSeconds;

    public AdminLoginLockedException(long retryAfterSeconds) {
        super("Admin login is temporarily locked.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
