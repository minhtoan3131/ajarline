package vn.io.ajarline.platform.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.io.Console;
import java.util.Arrays;

/**
 * A tool to create a password hash for the Admin account.
 * Runs separately without starting the Spring Boot application.
 */
public class AdminPasswordHashCommand {

    private AdminPasswordHashCommand(){

    }

    /**
     * Starts the tool, reads and confirms the password,
     * prints its hash, and clears the password data.
     */
    public static void main(String[] args) {
        if (args.length != 0) {
            throw new IllegalArgumentException("This command does not accept arguments.");
        }

        Console console = System.console();
        if (console == null) {
            throw new IllegalArgumentException("Run this command is an interactive terminal.");
        }

        char[] password = console.readPassword("New admin password: ");
        char[] confirmPassword = console.readPassword("Confirm admin password: ");
        try {
            if (
                    password == null
                            || confirmPassword == null
                            || !Arrays.equals(password, confirmPassword)
            ) {
                throw new IllegalArgumentException("Password confirm does not match.");
            }
            console.printf("%s%n",
                    encode(password, Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
            if (confirmPassword != null) {
                Arrays.fill(confirmPassword, '\0');
            }
        }

    }

    /**
     * Checks the password length and creates a password hash.
     *
     * @param password the password to hash
     * @param encoder the password hashing tool
     * @return the generated password hash
     */
    static String encode(char[] password, PasswordEncoder encoder) {
        if (password == null) {
            throw new IllegalArgumentException("Password is required.");
        }

        int length = Character.codePointCount(password, 0, password.length);
        if (length < 16 || length > 1024) {
            throw new IllegalArgumentException("Password must contain  16 to 1024 characters.");
        }
        return encoder.encode(new String(password));
    }
}
