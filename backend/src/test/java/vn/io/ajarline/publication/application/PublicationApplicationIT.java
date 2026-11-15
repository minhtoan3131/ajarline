package vn.io.ajarline.publication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import vn.io.ajarline.content.contract.ArticleContentInput;
import vn.io.ajarline.content.contract.ArticleCreator;
import vn.io.ajarline.content.contract.ArticleReader;
import vn.io.ajarline.content.contract.ArticleView;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.content.contract.PublicArticleView;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.publication.contract.ContentPublisher;
import vn.io.ajarline.publication.contract.ContentUnpublisher;
import vn.io.ajarline.publication.contract.PublicationFailureCode;
import vn.io.ajarline.publication.contract.PublicationOperationException;
import vn.io.ajarline.publication.contract.PublicationResult;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.MutableClock;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class PublicationApplicationIT {

    private static final Instant CREATED = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant FIRST_PUBLISH = Instant.parse("2026-11-01T01:00:00Z");
    private static final Instant UNPUBLISH = Instant.parse("2026-11-01T02:00:00Z");
    private static final Instant REPUBLISH = Instant.parse("2026-11-01T03:00:00Z");

    @Autowired private ArticleCreator creator;
    @Autowired private ArticleReader reader;
    @Autowired private ContentPublisher publisher;
    @Autowired private ContentUnpublisher unpublisher;
    @Autowired private MutableClock clock;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactions;

    @BeforeEach
    void resetDatabaseAndClock() {
        jdbc.update("DELETE FROM content_item");
        clock.set(CREATED);
    }

    @Test
    void publish_ShouldSetFirstPublicationState_WhenDraftIsValid() {
        ArticleView draft = create("publish-article", "Summary", "# Body");
        clock.set(FIRST_PUBLISH);

        PublicationResult result = publisher.publish(draft.id(), 1);

        assertThat(result.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(result.revision()).isEqualTo(2);
        assertThat(result.publishedAt()).isEqualTo(FIRST_PUBLISH);
        assertThat(result.unpublishedAt()).isNull();
        assertThat(result.updatedAt()).isEqualTo(FIRST_PUBLISH);
    }

    @Test
    void publish_ShouldRejectDraft_WhenSummaryOrBodyIsMissing() {
        ArticleView noSummary = create("no-summary", null, "# Body");
        ArticleView noBody = create("no-body", "Summary", null);

        assertPublicationFailure(
                () -> publisher.publish(noSummary.id(), 1),
                PublicationFailureCode.CONTENT_SUMMARY_REQUIRED);
        assertPublicationFailure(
                () -> publisher.publish(noBody.id(), 1),
                PublicationFailureCode.ARTICLE_BODY_REQUIRED);

        assertDraftUnchanged(noSummary.id());
        assertDraftUnchanged(noBody.id());
    }

    @Test
    void publish_ShouldBeIdempotent_WhenAlreadyPublished() {
        ArticleView draft = create("idempotent-publish", "Summary", "# Body");
        clock.set(FIRST_PUBLISH);
        PublicationResult first = publisher.publish(draft.id(), 1);
        clock.set(REPUBLISH);

        PublicationResult second = publisher.publish(draft.id(), 2);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void unpublish_ShouldRejectDraftWithoutMutation() {
        ArticleView draft = create("draft-unpublish", "Summary", "# Body");

        assertPublicationFailure(
                () -> unpublisher.unpublish(draft.id(), 1),
                PublicationFailureCode.INVALID_PUBLICATION_STATE);
        assertDraftUnchanged(draft.id());
    }

    @Test
    void lifecycle_ShouldPreserveFirstPublishAndLatestUnpublishTimes() {
        ArticleView draft = create("full-lifecycle", "Summary", "# Body");
        clock.set(FIRST_PUBLISH);
        publisher.publish(draft.id(), 1);

        clock.set(UNPUBLISH);
        PublicationResult unpublished = unpublisher.unpublish(draft.id(), 2);
        assertThat(unpublished.status()).isEqualTo(PublicationStatus.UNPUBLISHED);
        assertThat(unpublished.revision()).isEqualTo(3);
        assertThat(unpublished.publishedAt()).isEqualTo(FIRST_PUBLISH);
        assertThat(unpublished.unpublishedAt()).isEqualTo(UNPUBLISH);

        clock.set(REPUBLISH);
        PublicationResult republished = publisher.publish(draft.id(), 3);
        assertThat(republished.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(republished.revision()).isEqualTo(4);
        assertThat(republished.publishedAt()).isEqualTo(FIRST_PUBLISH);
        assertThat(republished.unpublishedAt()).isEqualTo(UNPUBLISH);
        assertThat(republished.updatedAt()).isEqualTo(REPUBLISH);
    }

    @Test
    void unpublish_ShouldBeIdempotent_WhenAlreadyUnpublished() {
        ArticleView draft = create("idempotent-unpublish", "Summary", "# Body");
        clock.set(FIRST_PUBLISH);
        publisher.publish(draft.id(), 1);
        clock.set(UNPUBLISH);
        PublicationResult first = unpublisher.unpublish(draft.id(), 2);
        clock.set(REPUBLISH);

        PublicationResult second = unpublisher.unpublish(draft.id(), 3);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void publish_ShouldRejectStaleRevision_EvenWhenCurrentStateIsPublished() {
        ArticleView draft = create("stale-idempotent", "Summary", "# Body");
        publisher.publish(draft.id(), 1);

        assertThatThrownBy(() -> publisher.publish(draft.id(), 1))
                .isInstanceOfSatisfying(
                        ContentOperationException.class,
                        failure -> assertThat(failure.code())
                                .isEqualTo(ContentFailureCode.REVISION_CONFLICT));
    }

    @Test
    void publish_ShouldRollbackCanonicalMutation_WhenTransactionLaterFails() {
        ArticleView draft = create("rollback-publish", "Summary", "# Body");
        clock.set(FIRST_PUBLISH);

        assertThatThrownBy(() -> transactions.executeWithoutResult(ignored -> {
            publisher.publish(draft.id(), 1);
            throw new ForcedRollbackException();
        })).isInstanceOf(ForcedRollbackException.class);

        assertDraftUnchanged(draft.id());
        assertThat(reader.findPublishedBySlug("rollback-publish")).isEmpty();
    }

    @Test
    void unpublish_ShouldRollbackAndKeepPublicEligibility_WhenTransactionFails() {
        ArticleView draft = create("rollback-unpublish", "Summary", "# Body");
        clock.set(FIRST_PUBLISH);
        publisher.publish(draft.id(), 1);
        clock.set(UNPUBLISH);

        assertThatThrownBy(() -> transactions.executeWithoutResult(ignored -> {
            unpublisher.unpublish(draft.id(), 2);
            throw new ForcedRollbackException();
        })).isInstanceOf(ForcedRollbackException.class);

        ArticleView stored = reader.get(draft.id());
        assertThat(stored.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(stored.revision()).isEqualTo(2);
        assertThat(stored.publishedAt()).isEqualTo(FIRST_PUBLISH);
        assertThat(stored.unpublishedAt()).isNull();
        assertThat(stored.updatedAt()).isEqualTo(FIRST_PUBLISH);
        assertThat(reader.findPublishedBySlug("rollback-unpublish").map(PublicArticleView::article))
                .contains(stored);
    }

    private ArticleView create(String slug, String summary, String body) {
        return creator.create(new ArticleContentInput(
                "Publication Article", slug, summary, body));
    }

    private void assertDraftUnchanged(java.util.UUID id) {
        ArticleView stored = reader.get(id);
        assertThat(stored.status()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(stored.revision()).isEqualTo(1);
        assertThat(stored.publishedAt()).isNull();
        assertThat(stored.unpublishedAt()).isNull();
        assertThat(stored.updatedAt()).isEqualTo(CREATED);
    }

    private void assertPublicationFailure(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable action,
            PublicationFailureCode code
    ) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(
                        PublicationOperationException.class,
                        failure -> assertThat(failure.code()).isEqualTo(code));
    }

    private static final class ForcedRollbackException
            extends RuntimeException {
    }
}
