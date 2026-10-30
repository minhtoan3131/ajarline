package vn.io.ajarline.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

class AdminPasswordHashCommandTest {

    @Test
    void encode_ShouldRejectPassword_WhenBelowCreationMinimum() {
        char[] password = new char[15];
        Arrays.fill(password, 'a');
        var encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        try {
            assertThatThrownBy(() -> AdminPasswordHashCommand.encode(password, encoder))
                    .isInstanceOf(IllegalArgumentException.class);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @Test
    void encode_ShouldProduceVerifiableHash_WhenAtCreationMinimum() {
        char[] password = new char[16];
        Arrays.fill(password, 'a');
        var encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

        try {
            String hash = AdminPasswordHashCommand.encode(password, encoder);
            assertThat(encoder.matches(new String(password), hash)).isTrue();
            assertThat(hash.length()).isEqualTo(96);
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
