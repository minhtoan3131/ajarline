package vn.io.ajarline.administration.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewsEditableTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void parse_ShouldReadEveryField_WhenBodyIsComplete() {
        NewsEditable editable = NewsEditable.parse(json(complete(
                """
                {"name": "A source", "url": "https://example.com", "publishedAt": "2026-11-18T10:00:00+07:00"}
                """)));

        assertThat(editable.title()).isEqualTo("A title");
        assertThat(editable.source().name()).isEqualTo("A source");
        assertThat(editable.source().url()).isEqualTo("https://example.com");
        assertThat(editable.source().publishedAt()).isEqualTo(Instant.parse("2026-11-18T03:00:00Z"));
    }

    @Test
    void parse_ShouldAcceptNulls_WhenDraftHasAnEmptySource() {
        NewsEditable editable = NewsEditable.parse(json(complete(
                "{\"name\": null, \"url\": null, \"publishedAt\": null}")));

        assertThat(editable.source().name()).isNull();
        assertThat(editable.source().url()).isNull();
        assertThat(editable.source().publishedAt()).isNull();
    }

    @Test
    void parse_ShouldRejectBody_WhenAKeyIsMissing() {
        String body = """
                {"title": "A title", "slug": "a-title", "summary": null,
                 "bodyMarkdown": null, "topicIds": []}
                """;

        assertInvalid(body);
    }

    @Test
    void parse_ShouldRejectBody_WhenAnUnknownKeyIsPresent() {
        String body = complete("{\"name\": null, \"url\": null, \"publishedAt\": null}")
                .replace("\"topicIds\": []", "\"topicIds\": [], \"extra\": 1");

        assertInvalid(body);
    }

    @ParameterizedTest(name = "[{index}] rejects source {0}")
    @ValueSource(strings = {
            "null",
            "\"https://example.com\"",
            "{\"name\": null, \"url\": null}",
            "{\"name\": null, \"url\": null, \"publishedAt\": null, \"extra\": 1}"})
    void parse_ShouldRejectBody_WhenSourceIsNotAnObjectWithThreeKeys(String source) {
        assertInvalid(complete(source));
    }

    @ParameterizedTest(name = "[{index}] rejects publishedAt {0}")
    @ValueSource(strings = {"\"2026-11-18\"", "\"2026-11-18T10:00\"", "\"yesterday\"", "20261118", "true"})
    void parse_ShouldRejectBody_WhenPublishedAtIsNotADateTimeWithOffset(String publishedAt) {
        assertInvalid(complete(
                "{\"name\": null, \"url\": null, \"publishedAt\": " + publishedAt + "}"));
    }

    @Test
    void parse_ShouldRejectBody_WhenSourceNameIsNotAString() {
        assertInvalid(complete("{\"name\": 5, \"url\": null, \"publishedAt\": null}"));
    }

    @Test
    void toContentInput_ShouldNestTheCommonFields_WhenBodyIsComplete() {
        NewsEditable editable = NewsEditable.parse(json(complete(
                "{\"name\": \"A source\", \"url\": null, \"publishedAt\": null}")));

        assertThat(editable.toContentInput().record().slug()).isEqualTo("a-title");
        assertThat(editable.toContentInput().source().name()).isEqualTo("A source");
    }

    private static String complete(String source) {
        return """
                {"title": "A title", "slug": "a-title", "summary": "A summary",
                 "bodyMarkdown": "# Body", "source": %s, "topicIds": []}
                """.formatted(source);
    }

    private static JsonNode json(String text) {
        return JSON.readTree(text);
    }

    private static void assertInvalid(String body) {
        assertThatThrownBy(() -> NewsEditable.parse(json(body)))
                .isInstanceOfSatisfying(AdminApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(400);
                    assertThat(exception.code()).isEqualTo("INVALID_REQUEST");
                });
    }
}
