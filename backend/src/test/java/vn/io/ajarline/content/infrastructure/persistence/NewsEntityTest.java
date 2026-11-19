package vn.io.ajarline.content.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NewsEntityTest {

    private static final Instant PUBLISHED = Instant.parse("2026-11-18T03:00:00Z");

    @Test
    void hasSameBody_ShouldBeTrue_WhenBothBodiesAreNull() {
        NewsEntity news = news("# Body");
        news.replaceBody(null);

        assertThat(news.hasSameBody(null)).isTrue();
    }

    @Test
    void hasSameBody_ShouldBeFalse_WhenTextIsDifferent() {
        NewsEntity news = news("# Body");

        assertThat(news.hasSameBody("# Other")).isFalse();
    }

    @Test
    void hasSameSource_ShouldBeTrue_WhenInstantIsTheSameWithAnotherOffset() {
        NewsEntity news = news("# Body");
        Instant sameMoment = OffsetDateTime.parse("2026-11-18T10:00:00+07:00").toInstant();

        assertThat(news.hasSameSource("Source", "https://example.com", sameMoment)).isTrue();
    }

    @Test
    void hasSameSource_ShouldBeFalse_WhenOneValueIsDifferent() {
        NewsEntity news = news("# Body");

        assertThat(news.hasSameSource("Other", "https://example.com", PUBLISHED)).isFalse();
        assertThat(news.hasSameSource("Source", "https://example.org", PUBLISHED)).isFalse();
        assertThat(news.hasSameSource("Source", "https://example.com", null)).isFalse();
    }

    @Test
    void replaceSource_ShouldChangeAllThreeValues_WhenCalled() {
        NewsEntity news = news("# Body");

        news.replaceSource(null, null, null);

        assertThat(news.hasSameSource(null, null, null)).isTrue();
    }

    private static NewsEntity news(String body) {
        return new NewsEntity(UUID.randomUUID(), body, "Source", "https://example.com", PUBLISHED);
    }
}
