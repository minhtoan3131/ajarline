package vn.io.ajarline.content.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentRecordPolicyTest {
    private final ContentRecordPolicy policy = new ContentRecordPolicy();

    @Test
    void requireValidTitle_ShouldRejectTitle_WhenTitleIsTooLong() {
        assertThatThrownBy(() -> policy.requireValidTitle("a".repeat(301)))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(ContentFailureCode.VALIDATION_FAILED));
    }

    @Test
    void normalizeSlug_ShouldTrimAndLowercase_WhenSlugIsValid() {
        assertThat(policy.normalizeSlug("  Java-THreads  ")).isEqualTo("java-threads");
    }

    @ParameterizedTest(name = "[{index}] rejects \"{0}\"")
    @ValueSource(strings = {"java threads", "-java", "java-", "java--threads", "java_threads"})
    void normalizeSlug_ShouldRejectSlug_WhenFormatIsInvalid(String slug) {
        assertThatThrownBy(() -> policy.normalizeSlug(slug))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(ContentFailureCode.VALIDATION_FAILED));
    }

    @Test
    void normalizeSlug_ShouldRejectSlug_WhenSlugIsTooLong() {
        assertThatThrownBy(() -> policy.normalizeSlug("s".repeat(181)))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(ContentFailureCode.VALIDATION_FAILED));
    }

    @Test
    void normalizeSummary_ShouldReturnNull_WhenSummaryIsEmpty() {
        assertThat(policy.normalizeSummary("")).isNull();
    }

    @Test
    void normalizeSummary_ShouldRejectSummary_WhenSummaryIsBlank() {
        assertThatThrownBy(() -> policy.normalizeSummary("   "))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(ContentFailureCode.VALIDATION_FAILED));
    }

    @Test
    void normalizeSummary_ShouldRejectSummary_WhenSummaryIsTooLong() {
        assertThatThrownBy(() -> policy.normalizeSummary("a".repeat(1001)))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(ContentFailureCode.VALIDATION_FAILED));
    }

    @ParameterizedTest(name = "[{index}] rejects \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void requirePublishableSummary_ShouldRejectMissingSummary(String summary) {
        assertThatThrownBy(() -> policy.requirePublishableSummary(summary))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(ContentFailureCode.CONTENT_SUMMARY_REQUIRED));
    }

    @Test
    void requirePublishableSummary_ShouldAcceptPresentSummary() {
        assertThatCode(() -> policy.requirePublishableSummary("A short summary."))
                .doesNotThrowAnyException();
    }
}
