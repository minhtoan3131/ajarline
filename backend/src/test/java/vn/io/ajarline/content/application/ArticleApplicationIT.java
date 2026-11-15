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
    private static final Instant CREATED_AT = Instant.parse("2026-10-26T00:00:00Z");

    @Autowired
    private ArticleCreator articleCreator;

    @Autowired
    private ArticleReader articleReader;

    @Autowired
    private ArticleEditor articleEditor;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabaseAndClock() {
        jdbcTemplate.update("DELETE FROM content_item");
        clock.set(CREATED_AT);
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
        assertThat(created.createdAt()).isEqualTo(CREATED_AT);
        assertThat(created.updatedAt()).isEqualTo(CREATED_AT);
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
                """, OffsetDateTime.ofInstant(CREATED_AT, ZoneOffset.UTC), createdArticle.id());

        ArticleView found = articleReader.findPublishedBySlug("published-article").orElseThrow().article();

        assertThat(found.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(found.bodyMarkdown()).isEqualTo("# body");
    }

    @Test
    void replace_ShouldAdvanceRevisionOne_WhenContentChanges() {
        ArticleView oldArticle = articleCreator.create(input("java-threads", "# Old body"));
        Instant changedAt = Instant.parse("2026-10-26T01:00:00Z");
        clock.set(changedAt);

        ArticleView newArticle = articleEditor.replace(oldArticle.id(), 1,
                input("java-threads", "# New body"));

        assertThat(newArticle.revision()).isEqualTo(2);
        assertThat(newArticle.bodyMarkdown()).isEqualTo("# New body");
        assertThat(newArticle.updatedAt()).isEqualTo(changedAt);
    }

    @Test
    void replace_ShouldKeepRevisionAndTime_WhenContentIsEffectiveNoOp() {
        ArticleView created = articleCreator.create(input("java-threads", "Same body"));
        Instant changedAt = Instant.parse("2026-10-26T01:00:00Z");
        clock.set(changedAt);

        ArticleView unchanged = articleEditor.replace(created.id(), 1,
                input("java-threads", "Same body"));

        assertThat(unchanged.revision()).isEqualTo(1);
        assertThat(unchanged.updatedAt()).isEqualTo(CREATED_AT);

    }

    @Test
    void replace_ShouldRejectMutation_WhenRevisionIsStale() {
        ArticleView created = articleCreator.create(input("java-threads", "Old body"));
        articleEditor.replace(created.id(), 1, input("java-threads", "New body"));

        assertThatThrownBy(() -> articleEditor.replace(created.id(), 1,
                input("java-threads", "Body xyz")))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(ContentFailureCode.REVISION_CONFLICT));

        assertThat(articleReader.get(created.id()).bodyMarkdown()).isEqualTo("New body");
    }

    @Test
    void replace_ShouldRejectSlugChange_WhenArticleWasPublishedBefore() {
        ArticleView created = articleCreator.create(input("java-threads", "Body"));
        jdbcTemplate.update("""
                UPDATE content_item
                SET status = 'UNPUBLISHED', published_at = ?
                WHERE id = ?
                """, OffsetDateTime.ofInstant(CREATED_AT, ZoneOffset.UTC), created.id());

        assertThatThrownBy(() -> articleEditor.replace(created.id(), 1,
                input("new-slug", "Body")))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        e -> assertThat(e.code())
                                .isEqualTo(ContentFailureCode.SLUG_IMMUTABLE_AFTER_PUBLICATION));
    }

    @Test
    void replace_ShouldRollbackContent_WhenSlugConflicts() {
        articleCreator.create(input("exist-slug", "Body"));
        ArticleView target = articleCreator.create(input("target-slug", "Original Body"));
        clock.set(Instant.parse("2026-10-26T03:00:00Z"));

        assertThatThrownBy(() -> articleEditor.replace(
                target.id(), 1, input(
                        "exist-slug", "Change Body"
                )
        )).isInstanceOfSatisfying(ContentOperationException.class,
                e -> assertThat(e.code()).isEqualTo(ContentFailureCode.SLUG_CONFLICT));

        ArticleView persisted = articleReader.get(target.id());
        assertThat(persisted.slug()).isEqualTo("target-slug");
        assertThat(persisted.bodyMarkdown()).isEqualTo("Original Body");
        assertThat(persisted.revision()).isEqualTo(1);
        assertThat(persisted.updatedAt()).isEqualTo(CREATED_AT);
    }


    private ArticleContentInput input(String slug, String bodyMarkdown) {
        return new ArticleContentInput("Java threads", slug, null, bodyMarkdown);
    }

    private long rowCount(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
