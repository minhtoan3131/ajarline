package vn.io.ajarline.support;

import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.content.contract.PublicationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

import static vn.io.ajarline.content.contract.PublicationStatus.DRAFT;
import static vn.io.ajarline.content.contract.PublicationStatus.PUBLISHED;
import static vn.io.ajarline.support.TopicTestData.uuid;

/**
 * Inserts Series, Modules and memberships with SQL, so tests can build any hierarchy state.
 * Every item has a summary, so it can be PUBLISHED. The title comes from the slug.
 */
public final class LearningTestData {

    // The standard hierarchy. Each hidden item is hidden for a different reason.

    /** Public. Its Modules in order: A1 (public), A3 (draft), A2 (public). */
    public static final UUID SERIES_A = uuid(2001);
    /** Published, but its only Module is a draft. Hidden. */
    public static final UUID SERIES_B = uuid(2002);
    /** A draft, but its Module is public. Hidden. */
    public static final UUID SERIES_C = uuid(2003);
    /** Published, but its only Module has no public Article. Hidden. */
    public static final UUID SERIES_D = uuid(2004);

    /** Public. Its Articles in order: A1 (public), A2 (draft), A3 (public). */
    public static final UUID MODULE_A1 = uuid(2011);
    /** Public. Its only Article is A4. It is the second public Module of Series A. */
    public static final UUID MODULE_A2 = uuid(2012);
    /** A draft, in the middle of Series A. Its Article A5 is published. */
    public static final UUID MODULE_A3 = uuid(2013);
    public static final UUID MODULE_B1 = uuid(2031);
    /** Public, under the hidden Series C. */
    public static final UUID MODULE_C1 = uuid(2051);
    /** Published, but its only Article is a draft. Hidden. */
    public static final UUID MODULE_D1 = uuid(2071);

    public static final UUID ARTICLE_A1 = uuid(2021);
    /** A draft, between two public Articles. */
    public static final UUID ARTICLE_A2 = uuid(2022);
    public static final UUID ARTICLE_A3 = uuid(2023);
    public static final UUID ARTICLE_A4 = uuid(2024);
    /** Published, but its Module A3 is a draft. */
    public static final UUID ARTICLE_A5 = uuid(2025);
    public static final UUID ARTICLE_B1 = uuid(2041);
    public static final UUID ARTICLE_C1 = uuid(2061);
    public static final UUID ARTICLE_D1 = uuid(2081);
    /** Published and in no Module. */
    public static final UUID ARTICLE_E = uuid(2091);

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.parse("2026-11-18T00:00:00Z");

    private LearningTestData() {
    }

    /**
     * Inserts the common record only, without a subtype row.
     */
    public static void insertContentItem(
            JdbcTemplate jdbcTemplate, UUID id, String type, String slug, PublicationStatus status) {
        OffsetDateTime publishedAt = status == PublicationStatus.DRAFT ? null : FIXED_TIME;
        OffsetDateTime unpublishedAt = status == PublicationStatus.UNPUBLISHED ? FIXED_TIME : null;
        jdbcTemplate.update("""
                INSERT INTO content_item(id, type, slug, title, summary, status, revision,
                    published_at, unpublished_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'Fixture summary', ?, 1, ?, ?, ?, ?)
                """, id, type, slug, "Fixture " + slug, status.name(),
                publishedAt, unpublishedAt, FIXED_TIME, FIXED_TIME);
    }

    public static void insertSeries(
            JdbcTemplate jdbcTemplate, UUID id, String slug,
            PublicationStatus status, String overviewMarkdown) {
        insertContentItem(jdbcTemplate, id, "SERIES", slug, status);
        jdbcTemplate.update("""
                INSERT INTO series(content_id, overview_markdown)
                VALUES (?, ?)
                """, id, overviewMarkdown);
    }

    public static void insertSeries(JdbcTemplate jdbcTemplate, UUID id, String slug) {
        insertSeries(jdbcTemplate, id, slug, PublicationStatus.DRAFT, "Fixture overview");
    }

    public static void insertModule(
            JdbcTemplate jdbcTemplate, UUID id, String slug, UUID seriesId, int position,
            PublicationStatus status, String overviewMarkdown) {
        insertContentItem(jdbcTemplate, id, "MODULE", slug, status);
        jdbcTemplate.update("""
                INSERT INTO module(content_id, series_id, position, overview_markdown)
                VALUES (?, ?, ?, ?)
                """, id, seriesId, position, overviewMarkdown);
    }

    public static void insertModule(
            JdbcTemplate jdbcTemplate, UUID id, String slug, UUID seriesId, int position) {
        insertModule(jdbcTemplate, id, slug, seriesId, position,
                PublicationStatus.DRAFT, "Fixture overview");
    }

    public static void insertModuleArticle(
            JdbcTemplate jdbcTemplate, UUID articleId, UUID moduleId, int position) {
        jdbcTemplate.update("""
                INSERT INTO module_article(article_id, module_id, position)
                VALUES (?, ?, ?)
                """, articleId, moduleId, position);
    }

    /**
     * Inserts an Article and puts it in a Module. The title is "Article " and the slug.
     */
    public static void insertArticleIn(
            JdbcTemplate jdbcTemplate, UUID id, String slug, PublicationStatus status,
            UUID moduleId, int position) {
        ContentTestData.insertArticle(jdbcTemplate, id, slug, "Article " + slug, status);
        insertModuleArticle(jdbcTemplate, id, moduleId, position);
    }

    /**
     * Inserts the standard hierarchy. The database must have no content.
     *
     * <p>Public now: Series A; Modules A1, A2, C1; Articles A1, A3, A4, C1 in them. Standalone for
     * readers: Articles A5, B1, E. The rest is hidden.</p>
     */
    public static void insertStandardScenarios(JdbcTemplate jdbcTemplate) {
        insertSeries(jdbcTemplate, SERIES_A, "series-a", PUBLISHED, "# Series A");
        insertModule(jdbcTemplate, MODULE_A1, "module-a1", SERIES_A, 1, PUBLISHED, "# Module A1");
        insertModule(jdbcTemplate, MODULE_A3, "module-a3", SERIES_A, 2, DRAFT, "# Module A3");
        insertModule(jdbcTemplate, MODULE_A2, "module-a2", SERIES_A, 3, PUBLISHED, "# Module A2");
        insertArticleIn(jdbcTemplate, ARTICLE_A1, "article-a1", PUBLISHED, MODULE_A1, 1);
        insertArticleIn(jdbcTemplate, ARTICLE_A2, "article-a2", DRAFT, MODULE_A1, 2);
        insertArticleIn(jdbcTemplate, ARTICLE_A3, "article-a3", PUBLISHED, MODULE_A1, 3);
        insertArticleIn(jdbcTemplate, ARTICLE_A4, "article-a4", PUBLISHED, MODULE_A2, 1);
        insertArticleIn(jdbcTemplate, ARTICLE_A5, "article-a5", PUBLISHED, MODULE_A3, 1);

        insertSeries(jdbcTemplate, SERIES_B, "series-b", PUBLISHED, "# Series B");
        insertModule(jdbcTemplate, MODULE_B1, "module-b1", SERIES_B, 1, DRAFT, "# Module B1");
        insertArticleIn(jdbcTemplate, ARTICLE_B1, "article-b1", PUBLISHED, MODULE_B1, 1);

        insertSeries(jdbcTemplate, SERIES_C, "series-c", DRAFT, "# Series C");
        insertModule(jdbcTemplate, MODULE_C1, "module-c1", SERIES_C, 1, PUBLISHED, "# Module C1");
        insertArticleIn(jdbcTemplate, ARTICLE_C1, "article-c1", PUBLISHED, MODULE_C1, 1);

        insertSeries(jdbcTemplate, SERIES_D, "series-d", PUBLISHED, "# Series D");
        insertModule(jdbcTemplate, MODULE_D1, "module-d1", SERIES_D, 1, PUBLISHED, "# Module D1");
        insertArticleIn(jdbcTemplate, ARTICLE_D1, "article-d1", DRAFT, MODULE_D1, 1);

        ContentTestData.insertArticle(jdbcTemplate, ARTICLE_E, "article-e", "Article article-e", PUBLISHED);
    }

    /**
     * Deletes all content and hierarchy rows, children first. The hierarchy foreign keys are RESTRICT.
     */
    public static void deleteAll(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM module_article");
        jdbcTemplate.update("DELETE FROM module");
        jdbcTemplate.update("DELETE FROM series");
        jdbcTemplate.update("DELETE FROM content_item");
    }
}
