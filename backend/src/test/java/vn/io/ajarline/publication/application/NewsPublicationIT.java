package vn.io.ajarline.publication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.publication.contract.ContentPublisher;
import vn.io.ajarline.publication.contract.ContentUnpublisher;
import vn.io.ajarline.publication.contract.PublicationResult;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.ContentTestData;
import vn.io.ajarline.support.MutableClock;
import vn.io.ajarline.support.NewsTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest
@Import({PostgreSqlTestConfiguration.class, ContentApplicationTestConfiguration.class})
class NewsPublicationIT {

    private static final Instant CREATED = Instant.parse("2026-11-19T00:00:00Z");
    private static final Instant FIRST_PUBLISH = Instant.parse("2026-11-19T01:00:00Z");
    private static final Instant UNPUBLISH = Instant.parse("2026-11-19T02:00:00Z");
    private static final Instant REPUBLISH = Instant.parse("2026-11-19T03:00:00Z");

    private static final UUID NEWS_ID = uuid(1);
    private static final UUID ARTICLE_ID = uuid(2);

    @Autowired private ContentPublisher publisher;
    @Autowired private ContentUnpublisher unpublisher;
    @Autowired private MutableClock clock;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void resetDatabaseAndClock() {
        jdbc.update("DELETE FROM content_item");
        clock.set(CREATED);
    }

    @Test
    void publish_ShouldPublishNews_WhenSummaryBodyAndSourceArePresent() {
        NewsTestData.insertCompleteDraft(jdbc, NEWS_ID, "a-complete-news");
        clock.set(FIRST_PUBLISH);

        PublicationResult result = publisher.publish(NEWS_ID, 1);

        assertThat(result.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(result.revision()).isEqualTo(2);
        assertThat(result.publishedAt()).isEqualTo(FIRST_PUBLISH);
    }

    @Test
    void publish_ShouldReportSummaryFirst_WhenSummaryBodyAndSourceAreMissing() {
        NewsTestData.insertNews(jdbc, NEWS_ID, "nothing-yet", PublicationStatus.DRAFT,
                null, null, null, null);

        assertContentFailure(NEWS_ID, 1, ContentFailureCode.CONTENT_SUMMARY_REQUIRED);
        assertStillDraft(NEWS_ID);
    }

    @Test
    void publish_ShouldReportBodyBeforeSource_WhenBodyAndSourceAreMissing() {
        NewsTestData.insertNews(jdbc, NEWS_ID, "no-body", PublicationStatus.DRAFT,
                "Summary", null, null, null);

        assertContentFailure(NEWS_ID, 1, ContentFailureCode.NEWS_BODY_REQUIRED);
        assertStillDraft(NEWS_ID);
    }

    @Test
    void publish_ShouldRequireSource_WhenSourceNameIsMissing() {
        NewsTestData.insertNews(jdbc, NEWS_ID, "no-source-name", PublicationStatus.DRAFT,
                "Summary", "# Body", null, "https://example.com/story");

        assertContentFailure(NEWS_ID, 1, ContentFailureCode.NEWS_SOURCE_REQUIRED);
        assertStillDraft(NEWS_ID);
    }

    @Test
    void publish_ShouldRequireSource_WhenSourceUrlIsMissing() {
        NewsTestData.insertNews(jdbc, NEWS_ID, "no-source-url", PublicationStatus.DRAFT,
                "Summary", "# Body", "A source", null);

        assertContentFailure(NEWS_ID, 1, ContentFailureCode.NEWS_SOURCE_REQUIRED);
        assertStillDraft(NEWS_ID);
    }

    @Test
    void publish_ShouldRequireSource_WhenSourceUrlHasASpace() {
        // The database check allows this value. Only the application rule rejects it.
        NewsTestData.insertNews(jdbc, NEWS_ID, "bad-source-url", PublicationStatus.DRAFT,
                "Summary", "# Body", "A source", "https://exa mple.com/story");

        assertContentFailure(NEWS_ID, 1, ContentFailureCode.NEWS_SOURCE_REQUIRED);
        assertStillDraft(NEWS_ID);
    }

    @Test
    void publish_ShouldAcceptReturnedRevision_WhenNewsIsUnpublishedNext() {
        NewsTestData.insertCompleteDraft(jdbc, NEWS_ID, "revision-chain");
        PublicationResult published = publisher.publish(NEWS_ID, 1);

        PublicationResult unpublished = unpublisher.unpublish(NEWS_ID, published.revision());

        assertThat(unpublished.status()).isEqualTo(PublicationStatus.UNPUBLISHED);
        assertThat(unpublished.revision()).isEqualTo(3);
    }

    @Test
    void publish_ShouldRejectStaleRevision_WhenRevisionIsOld() {
        NewsTestData.insertCompleteDraft(jdbc, NEWS_ID, "stale-revision");
        publisher.publish(NEWS_ID, 1);

        assertContentFailure(NEWS_ID, 1, ContentFailureCode.REVISION_CONFLICT);
    }

    @Test
    void publish_ShouldBeIdempotent_WhenNewsIsAlreadyPublished() {
        NewsTestData.insertCompleteDraft(jdbc, NEWS_ID, "publish-twice");
        clock.set(FIRST_PUBLISH);
        PublicationResult first = publisher.publish(NEWS_ID, 1);
        clock.set(REPUBLISH);

        PublicationResult second = publisher.publish(NEWS_ID, 2);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void lifecycle_ShouldKeepFirstPublishTime_WhenNewsIsPublishedAgain() {
        NewsTestData.insertCompleteDraft(jdbc, NEWS_ID, "full-lifecycle");
        clock.set(FIRST_PUBLISH);
        publisher.publish(NEWS_ID, 1);
        clock.set(UNPUBLISH);
        unpublisher.unpublish(NEWS_ID, 2);
        clock.set(REPUBLISH);

        PublicationResult republished = publisher.publish(NEWS_ID, 3);

        assertThat(republished.revision()).isEqualTo(4);
        assertThat(republished.publishedAt()).isEqualTo(FIRST_PUBLISH);
        assertThat(republished.unpublishedAt()).isEqualTo(UNPUBLISH);
    }

    @Test
    void unpublish_ShouldChangeOnlyThatNews_WhenAnArticleIsPublic() {
        ContentTestData.insertArticle(
                jdbc, ARTICLE_ID, "a-public-article", "A public article", PublicationStatus.PUBLISHED);
        NewsTestData.insertNews(jdbc, NEWS_ID, "a-public-news", PublicationStatus.PUBLISHED,
                "Summary", "# Body", "A source", "https://example.com/story");

        unpublisher.unpublish(NEWS_ID, 1);

        Map<String, Object> article = jdbc.queryForMap(
                "SELECT status, revision FROM content_item WHERE id = ?", ARTICLE_ID);
        assertThat(article.get("status")).isEqualTo("PUBLISHED");
        assertThat(article.get("revision")).isEqualTo(1L);
    }

    private void assertContentFailure(UUID id, long revision, ContentFailureCode code) {
        assertThatThrownBy(() -> publisher.publish(id, revision))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        exception -> assertThat(exception.code()).isEqualTo(code));
    }

    private void assertStillDraft(UUID id) {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, revision FROM content_item WHERE id = ?", id);
        assertThat(row.get("status")).isEqualTo("DRAFT");
        assertThat(row.get("revision")).isEqualTo(1L);
    }
}
