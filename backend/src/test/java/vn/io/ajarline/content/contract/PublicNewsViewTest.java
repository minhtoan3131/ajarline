package vn.io.ajarline.content.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PublicNewsViewTest {

    private static final Instant TIME = Instant.parse("2026-11-19T00:00:00Z");

    @Test
    void constructor_ShouldAccept_WhenNewsIsCompleteAndSourceHasNoDate() {
        PublicNewsView view = view("A summary", "# Body", "Source", "https://example.com/a");

        assertThat(view.source().publishedAt()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void constructor_ShouldReject_WhenSummaryIsBlank(String summary) {
        assertThatThrownBy(() -> view(summary, "# Body", "Source", "https://example.com/a"))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void constructor_ShouldReject_WhenBodyIsBlank(String body) {
        assertThatThrownBy(() -> view("A summary", body, "Source", "https://example.com/a"))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void constructor_ShouldReject_WhenSourceNameIsBlank(String name) {
        assertThatThrownBy(() -> view("A summary", "# Body", name, "https://example.com/a"))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void constructor_ShouldReject_WhenSourceUrlIsBlank(String url) {
        assertThatThrownBy(() -> view("A summary", "# Body", "Source", url))
                .isInstanceOf(IllegalStateException.class);
    }

    private static PublicNewsView view(
            String summary, String body, String sourceName, String sourceUrl) {
        return new PublicNewsView(
                UUID.randomUUID(), "a-news", "A news", summary, body,
                new NewsSource(sourceName, sourceUrl, null),
                List.of(), TIME, TIME);
    }
}
