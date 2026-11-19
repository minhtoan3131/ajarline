package vn.io.ajarline.content.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SourceUrlPolicyTest {

    private final SourceUrlPolicy policy = new SourceUrlPolicy();

    // Keep this list equal to the "valid" list in urlPolicyTest.ts. The renderer must keep
    // every link that the server accepts.
    @ParameterizedTest(name = "[{index}] accepts {0}")
    @ValueSource(strings = {
            "http://example.com",
            "https://example.com",
            "HTTPS://EXAMPLE.COM/Path",
            "https://example.com:8080/a/b?x=1&y=2#top",
            "https://user:secret@example.com/x",
            "https://[::1]/status",
            "https://example.com/a|b{c}",
            "https://example.com/bai-viet/tiếng-việt"})
    void isValid_ShouldBeTrue_WhenUrlIsAbsoluteHttpOrHttps(String url) {
        assertThat(policy.isValid(url)).isTrue();
    }

    @ParameterizedTest(name = "[{index}] rejects {0}")
    @ValueSource(strings = {
            "javascript:alert(1)",
            "JAVASCRIPT:alert(1)",
            "data:text/html,<b>x</b>",
            "ftp://example.com/file",
            "mailto:a@example.com",
            "//example.com/path",
            "/relative/path",
            "example.com",
            "https:/example.com",
            "https//example.com",
            "https://",
            "https:///path",
            "https://:8080/path",
            "https://?q=1",
            "https://exa mple.com",
            " https://example.com",
            "https://example.com/a\tb",
            "https://example.com/a\nb",
            "https://example.com\\evil.com",
            "https://example.com/\u007f"})
    void isValid_ShouldBeFalse_WhenUrlIsNotSafe(String url) {
        assertThat(policy.isValid(url)).isFalse();
    }

    @Test
    void isValid_ShouldBeFalse_WhenUrlIsNullOrEmpty() {
        assertThat(policy.isValid(null)).isFalse();
        assertThat(policy.isValid("")).isFalse();
    }
}
