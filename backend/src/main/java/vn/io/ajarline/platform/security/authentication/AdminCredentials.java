package vn.io.ajarline.platform.security.authentication;

public record AdminCredentials(
        String username,
        String passwordHash
) {
    public AdminCredentials {
        if (username == null
                || username.isBlank()
                || username.codePointCount(0, username.length()) > 200) {
            throw new IllegalArgumentException("Admin username configuration is invalid.");
        }

        if (passwordHash == null || !passwordHash.matches("[0-9a-fA-F]{96}")) {
            throw new IllegalArgumentException("Admin password hash configuration is invalid.");
        }
    }

    @Override
    public String toString() {
        return "AdminCredentials[redacted]";
    }
}
