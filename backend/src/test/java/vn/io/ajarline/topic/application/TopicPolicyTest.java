package vn.io.ajarline.topic.application;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.io.ajarline.topic.contract.TopicCreateInput;
import vn.io.ajarline.topic.contract.TopicFailureCode;
import vn.io.ajarline.topic.contract.TopicOperationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TopicPolicyTest {

    private final TopicPolicy policy = new TopicPolicy();

    @Test
    void normalizeCreate_ShouldTrimAndLowercaseSlug_WhenSlugHasSpacesAndCapitals() {
        TopicCreateInput result = policy.normalizeCreate(
                new TopicCreateInput("  Java-Basics ", "Java basics", "Core Java.", true));

        assertThat(result.slug()).isEqualTo("java-basics");
        assertThat(result.name()).isEqualTo("Java basics");
        assertThat(result.description()).isEqualTo("Core Java.");
        assertThat(result.active()).isTrue();
    }

    @ParameterizedTest(name = "[{index}] rejects slug \"{0}\"")
    @ValueSource(strings = {"", "   ", "java_basics", "-java", "java-", "java--basics", "java basics", "java.basics"})
    void normalizeCreate_ShouldRejectSlug_WhenFormatIsInvalid(String slug) {
        assertValidationFailure(() -> policy.normalizeCreate(
                new TopicCreateInput(slug, "Name", null, true)));
    }

    @Test
    void normalizeCreate_ShouldRejectSlug_WhenSlugIsMissing() {
        assertValidationFailure(() -> policy.normalizeCreate(
                new TopicCreateInput(null, "Name", null, true)));
    }

    @Test
    void normalizeCreate_ShouldAcceptSlug_WhenSlugHasExactlyMaximumLength() {
        String slug = "a".repeat(160);

        TopicCreateInput result = policy.normalizeCreate(new TopicCreateInput(slug, "Name", null, true));

        assertThat(result.slug()).isEqualTo(slug);
    }

    @Test
    void normalizeCreate_ShouldRejectSlug_WhenSlugExceedsMaximumLength() {
        String slug = "a".repeat(161);

        assertValidationFailure(() -> policy.normalizeCreate(
                new TopicCreateInput(slug, "Name", null, true)));
    }

    @Test
    void normalizeCreate_ShouldRejectName_WhenNameIsMissingOrBlank() {
        assertValidationFailure(() -> policy.normalizeCreate(
                new TopicCreateInput("a-slug", null, null, true)));
        assertValidationFailure(() -> policy.normalizeCreate(
                new TopicCreateInput("a-slug", "   ", null, true)));
    }

    @Test
    void normalizeCreate_ShouldCountCharactersNotUtf16Units_WhenNameHasEmoji() {
        String name = "😀".repeat(200);

        TopicCreateInput result = policy.normalizeCreate(new TopicCreateInput("a-slug", name, null, true));

        assertThat(result.name()).isEqualTo(name);
    }

    @Test
    void normalizeCreate_ShouldRejectName_WhenNameExceedsMaximumLength() {
        String name = "a".repeat(201);

        assertValidationFailure(() -> policy.normalizeCreate(
                new TopicCreateInput("a-slug", name, null, true)));
    }

    @Test
    void normalizeCreate_ShouldConvertEmptyDescriptionToNull_WhenDescriptionIsEmpty() {
        TopicCreateInput result = policy.normalizeCreate(new TopicCreateInput("a-slug", "Name", "", true));

        assertThat(result.description()).isNull();
    }

    @Test
    void normalizeCreate_ShouldRejectDescription_WhenDescriptionIsBlankOrTooLong() {
        assertValidationFailure(() -> policy.normalizeCreate(
                new TopicCreateInput("a-slug", "Name", "   ", true)));
        String tooLong = "a".repeat(4001);
        assertValidationFailure(() -> policy.normalizeCreate(
                new TopicCreateInput("a-slug", "Name", tooLong, true)));
    }

    @Test
    void normalizeCreate_ShouldRejectInput_WhenInputIsNull() {
        assertValidationFailure(() -> policy.normalizeCreate(null));
    }

    private static void assertValidationFailure(ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(TopicOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(TopicFailureCode.VALIDATION_FAILED));
    }
}
