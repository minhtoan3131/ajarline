package vn.io.ajarline.support;

import java.util.UUID;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import vn.io.ajarline.platform.security.authentication.AdminCredentials;

/** Creates an ephemeral test identity without storing a usable password in source. */
public final class AdminAuthenticationTestData {

    private static final String USERNAME = "security-test-admin";
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String PASSWORD_HASH =
            Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(PASSWORD);

    private AdminAuthenticationTestData() {
    }

    public static String username() {
        return USERNAME;
    }

    public static String password() {
        return PASSWORD;
    }

    public static String passwordHash() {
        return PASSWORD_HASH;
    }

    public static AdminCredentials credentials() {
        return new AdminCredentials(USERNAME, PASSWORD_HASH);
    }
}
