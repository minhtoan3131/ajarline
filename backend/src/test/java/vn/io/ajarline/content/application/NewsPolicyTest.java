package vn.io.ajarline.content.application;

import org.junit.jupiter.api.Test;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;

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

    private static void assertFailure(Runnable action, ContentFailureCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ContentOperationException.class,
                        exception -> assertThat(exception.code()).isEqualTo(code));
    }
}
