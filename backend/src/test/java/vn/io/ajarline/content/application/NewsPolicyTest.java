package vn.io.ajarline.content.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.content.contract.NewsSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewsPolicyTest {

    private final NewsPolicy policy = new NewsPolicy(new ContentRecordPolicy(), new SourceUrlPolicy());

    @Test
    void requirePublishable_ShouldPass_WhenEverythingIsPresent() {
        assertThatCode(() -> policy.requirePublishable(
                "Summary", "# Body", "A source", "https://example.com/story"))
                .doesNotThrowAnyException();
    }

    @Test
    void requirePublishable_ShouldRequireSummaryFirst_WhenEverythingIsMissing() {
        assertFailure(() -> policy.requirePublishable(null, null, null, null),
                ContentFailureCode.CONTENT_SUMMARY_REQUIRED);
    }

    @Test
    void requirePublishable_ShouldRequireBodyBeforeSource_WhenBodyAndSourceAreMissing() {
        assertFailure(() -> policy.requirePublishable("Summary", "  ", null, null),
                ContentFailureCode.NEWS_BODY_REQUIRED);
    }

    @Test
    void requirePublishable_ShouldRequireSource_WhenSourceNameIsMissing() {
        assertFailure(() -> policy.requirePublishable(
                        "Summary", "# Body", null, "https://example.com"),
                ContentFailureCode.NEWS_SOURCE_REQUIRED);
    }

    @Test
    void requirePublishable_ShouldRequireSource_WhenSourceUrlIsMissing() {
        assertFailure(() -> policy.requirePublishable("Summary", "# Body", "A source", null),
                ContentFailureCode.NEWS_SOURCE_REQUIRED);
    }

    @Test
    void requirePublishable_ShouldRequireSource_WhenSourceUrlIsNotValid() {
        assertFailure(() -> policy.requirePublishable(
                        "Summary", "# Body", "A source", "https://exa mple.com"),
                ContentFailureCode.NEWS_SOURCE_REQUIRED);
    }

    @Test
    void normalizeSource_ShouldTrimAndEmptyToNull_WhenPartsAreBlank() {
        NewsSource result = policy.normalizeSource(new NewsSource("  A source  ", "   ", null));

        assertThat(result.name()).isEqualTo("A source");
        assertThat(result.url()).isNull();
        assertThat(result.publishedAt()).isNull();
    }

    @Test
    void normalizeSource_ShouldAllowNameWithoutUrl_WhenDraftHasOnlyAName() {
        NewsSource result = policy.normalizeSource(new NewsSource("A source", null, null));

        assertThat(result.name()).isEqualTo("A source");
        assertThat(result.url()).isNull();
    }

    @Test
    void normalizeSource_ShouldTrimUrl_WhenUrlHasOuterSpaces() {
        NewsSource result = policy.normalizeSource(
                new NewsSource(null, "  https://example.com/story  ", null));

        assertThat(result.url()).isEqualTo("https://example.com/story");
    }

    @Test
    void normalizeSource_ShouldCutTimeToMicroseconds_WhenTimeHasNanoseconds() {
        Instant withNanos = Instant.parse("2026-11-18T03:00:00.123456789Z");

        NewsSource result = policy.normalizeSource(new NewsSource(null, null, withNanos));

        assertThat(result.publishedAt()).isEqualTo(Instant.parse("2026-11-18T03:00:00.123456Z"));
    }

    @ParameterizedTest(name = "[{index}] rejects {0}")
    @ValueSource(strings = {
            "javascript:alert(1)", "data:text/html,x", "ftp://example.com",
            "//example.com", "example.com", "https://exa mple.com"})
    void normalizeSource_ShouldRejectUrl_WhenUrlIsNotSafe(String url) {
        assertFailure(() -> policy.normalizeSource(new NewsSource(null, url, null)),
                ContentFailureCode.VALIDATION_FAILED);
    }

    @Test
    void normalizeSource_ShouldRejectName_WhenNameIsTooLong() {
        assertFailure(() -> policy.normalizeSource(new NewsSource("a".repeat(301), null, null)),
                ContentFailureCode.VALIDATION_FAILED);
    }

    @Test
    void normalizeSource_ShouldRejectUrl_WhenUrlIsTooLong() {
        String longUrl = "https://example.com/" + "a".repeat(2048);

        assertFailure(() -> policy.normalizeSource(new NewsSource(null, longUrl, null)),
                ContentFailureCode.VALIDATION_FAILED);
    }

    @Test
    void normalizeSource_ShouldRejectSource_WhenSourceIsNull() {
        assertFailure(() -> policy.normalizeSource(null), ContentFailureCode.VALIDATION_FAILED);
    }

    private static void assertFailure(Runnable action, ContentFailureCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ContentOperationException.class,
                        exception -> assertThat(exception.code()).isEqualTo(code));
    }
}
