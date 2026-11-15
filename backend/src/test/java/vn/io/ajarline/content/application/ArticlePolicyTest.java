package vn.io.ajarline.content.application;

import org.junit.jupiter.api.Test;
import vn.io.ajarline.content.contract.ArticleContentInput;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ArticlePolicyTest {
    private final ArticlePolicy articlePolicy = new ArticlePolicy();

    @Test
    void normalizeAndValidate_ShouldNormalizeSlug_WhenInputIsValid() {
        ArticleContentInput input = input("  JAva-THreaDs   ", null, null);
        ArticleContentInput result = articlePolicy.normalizeAndValidate(input);
        assertThat(result.slug()).isEqualTo("java-threads");
    }

    @Test
    void normalizeAndValidate_ShouldAllowNullableContent_WhenDraftIsIncomplete() {
        ArticleContentInput input = input("java-threads", null, null);
        ArticleContentInput result = articlePolicy.normalizeAndValidate(input);
        assertThat(result.summary()).isNull();
        assertThat(result.bodyMarkdown()).isNull();
    }

    @Test
    void normalizeAndValidate_ShouldRejectInput_WhenTitleIsBlank() {
        ArticleContentInput input = new ArticleContentInput(" ", "invalid-title", null, null);
        assertValidationFailure(input);
    }

    @Test
    void normalizeAndValidate_ShouldRejectInput_WhenTitleIsTooLong() {
        ArticleContentInput input = new ArticleContentInput("a".repeat(301), "invalid-title",
                null, null);
        assertValidationFailure(input);
    }

    @Test
    void normalizeAndValidate_ShouldRejectInput_WhenSlugIsInvalid() {
        ArticleContentInput input = input("java threads", null, null);
        assertValidationFailure(input);
    }

    @Test
    void normalizeAndValidate_ShouldRejectInput_WhenSlugIsTooLong() {
        ArticleContentInput input = input("s".repeat(181), null, null);
        assertValidationFailure(input);
    }

    @Test
    void normalizeAndValidate_ShouldRejectInput_WhenSummaryIsTooLong() {
        ArticleContentInput input = input("java-threads", "a".repeat(1001), null);
        assertValidationFailure(input);
    }

    @Test
    void normalizeAndValidate_ShouldRejectInput_WhenTopicIdsRepeat() {
        UUID topicId = UUID.randomUUID();
        ArticleContentInput input = new ArticleContentInput(
                "Java threads", "java-threads", null, null, List.of(topicId, topicId));

        assertValidationFailure(input);
    }

    @Test
    void normalizeAndValidate_ShouldKeepTopicIds_WhenInputIsValid() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ArticleContentInput input = new ArticleContentInput(
                "Java threads", "java-threads", null, null, List.of(first, second));

        ArticleContentInput result = articlePolicy.normalizeAndValidate(input);

        assertThat(result.topicIds()).containsExactly(first, second);
    }

    private ArticleContentInput input(
            String slug,
            String summary,
            String bodyMarkdown
    ) {
        return new ArticleContentInput("Java threads", slug, summary, bodyMarkdown);
    }

    private void assertValidationFailure(ArticleContentInput input) {
        assertThatThrownBy(() -> articlePolicy.normalizeAndValidate(input))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(ContentFailureCode.VALIDATION_FAILED));
    }
}
