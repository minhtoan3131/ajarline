package vn.io.ajarline.content.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.content.contract.*;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.MutableClock;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(
        {PostgreSqlTestConfiguration.class,
                ContentApplicationTestConfiguration.class}
)
public class ArticleApplicationIT {
    private static final Instant CREATE_AT = Instant.parse("2026-10-26T00:00:00Z");

    @Autowired
    private ArticleCreator articleCreator;

    @Autowired
    private ArticleReader articleReader;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabaseAndClock() {
        jdbcTemplate.update("DELETE FROM content_item");
        clock.set(CREATE_AT);
    }

    @Test
    void create_ShouldPersistDraftRevisionOne_WhenContentIsValid() {
        ArticleContentInput input = input("java-threads", null);

        ArticleView created = articleCreator.create(input);

        assertThat(created.id()).isNotNull();
        assertThat(created.type()).isEqualTo(ContentType.ARTICLE);
        assertThat(created.status()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(created.revision()).isEqualTo(1);
        assertThat(created.publishedAt()).isNull();
        assertThat(created.unpublishedAt()).isNull();
        assertThat(created.createdAt()).isEqualTo(CREATE_AT);
        assertThat(created.updatedAt()).isEqualTo(CREATE_AT);
        assertThat(rowCount("content_item")).isEqualTo(1);
        assertThat(rowCount("article")).isEqualTo(1);
    }

    @Test
    void creat_ShouldRejectSecondArticle_WhenSlugAlreadyExists() {
        articleCreator.create(input("java-threads", null));

        assertThatThrownBy(() -> articleCreator.create(input("java-threads", null)))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(ContentFailureCode.SLUG_CONFLICT));
        assertThat(rowCount("content_item")).isEqualTo(1);
        assertThat(rowCount("article")).isEqualTo(1);
    }

    @Test
    void get_ShouldReturnCanonicalState_WhenArticleExists() {
        ArticleView created = articleCreator.create(input("java-threas", "# Body"));

        ArticleView found = articleReader.get(created.id());

        assertThat(found).isEqualTo(created);
    }

    @Test
    void get_ShouldReportNotFound_WhenArticleDoesNotExist() {
        UUID missingId = UUID.fromString(
                "00000000-0000-0000-0000-000000000404");
        assertThatThrownBy(() -> articleReader.get(missingId))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(ContentFailureCode.CONTENT_NOT_FOUND));
    }

    @Test
    void findPublishedBySlug_ShouldReturnEmpty_WhenArticleIsDraft() {
        articleCreator.create(input("draft-article", null));
        assertThat(articleReader.findPublishedBySlug("draft-article")).isEmpty();
    }

    @Test
    void findPublishedBySlug_ShouldReturnArticle_WhenArticleIsPublished() {
        ArticleView createdArticle = articleCreator.create(input("published-article", "# body"));
        jdbcTemplate.update("""
                UPDATE content_item
                SET status = 'PUBLISHED', published_at = ?
                WHERE id = ?
                """, OffsetDateTime.ofInstant(CREATE_AT, ZoneOffset.UTC), createdArticle.id());

        ArticleView found = articleReader.findPublishedBySlug("published-article").orElseThrow();

        assertThat(found.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(found.bodyMarkdown()).isEqualTo("# body");
    }

    private ArticleContentInput input(String slug, String bodyMarkdown) {
        return new ArticleContentInput("Java threads", slug, null, bodyMarkdown);
    }

    private long rowCount(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
