package vn.io.ajarline.support;

import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.content.contract.PublicationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Inserts Series, Modules and memberships with SQL, so tests can build any hierarchy state.
 * Every item has a summary, so it can be PUBLISHED. The title comes from the slug.
 */
public final class LearningTestData {

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
     * Deletes all content and hierarchy rows, children first. The hierarchy foreign keys are RESTRICT.
     */
    public static void deleteAll(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM module_article");
        jdbcTemplate.update("DELETE FROM module");
        jdbcTemplate.update("DELETE FROM series");
        jdbcTemplate.update("DELETE FROM content_item");
    }
}
