package vn.io.ajarline.publication.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.io.ajarline.content.contract.ArticleView;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.publication.contract.PublicationFailureCode;
import vn.io.ajarline.publication.contract.PublicationOperationException;

class ArticlePublicationPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");
    private final ArticlePublicationPolicy policy = new ArticlePublicationPolicy();

    @Test
    void validateForPublish_ShouldAcceptArticle_WhenRequiredFieldsArePresent() {
        assertThatCode(() -> policy.validateForPublish(
                article("Summary", "# Body")))
                .doesNotThrowAnyException();
    }

    @Test
    void validateForPublish_ShouldRequireSummary_WhenSummaryIsBlank() {
        assertFailure(article(" ", "# Body"),
                PublicationFailureCode.CONTENT_SUMMARY_REQUIRED);
    }

    @Test
    void validateForPublish_ShouldRequireBody_WhenBodyIsBlank() {
        assertFailure(article("Summary", " "),
                PublicationFailureCode.ARTICLE_BODY_REQUIRED);
    }

    private void assertFailure(
            ArticleView article,
            PublicationFailureCode expectedCode
    ) {
        assertThatThrownBy(() -> policy.validateForPublish(article))
                .isInstanceOfSatisfying(
                        PublicationOperationException.class,
                        failure -> org.assertj.core.api.Assertions
                                .assertThat(failure.code())
                                .isEqualTo(expectedCode));
    }

    private ArticleView article(String summary, String body) {
        return new ArticleView(
                UUID.randomUUID(),
                ContentType.ARTICLE,
                "Title",
                "article-slug",
                summary,
                body,
                PublicationStatus.DRAFT,
                1,
                null,
                null,
                NOW,
                NOW);
    }
}
