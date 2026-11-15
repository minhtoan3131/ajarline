package vn.io.ajarline.support;

import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.content.contract.PublicationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Inserts Articles directly through SQL so tests can set any publication
 * state without running the publication workflow.
 */
public final class ContentTestData {

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.parse("2026-11-12T00:00:00Z");

    private ContentTestData() {
    }

    /**
     * Inserts an Article with a summary and a body. A published or unpublished
     * Article gets a publication time, as the publication workflow would set.
     */
    public static void insertArticle(
            JdbcTemplate jdbcTemplate, UUID id, String slug, String title, PublicationStatus status) {
        OffsetDateTime publishedAt = status == PublicationStatus.DRAFT ? null : FIXED_TIME;
        OffsetDateTime unpublishedAt = status == PublicationStatus.UNPUBLISHED ? FIXED_TIME : null;
        jdbcTemplate.update("""
                INSERT INTO content_item(id, type, slug, title, summary, status, revision,
                    published_at, unpublished_at, created_at, updated_at)
                VALUES (?, 'ARTICLE', ?, ?, 'Fixture summary', ?, 1, ?, ?, ?, ?)
                """, id, slug, title, status.name(), publishedAt, unpublishedAt, FIXED_TIME, FIXED_TIME);
        jdbcTemplate.update("""
                INSERT INTO article(content_id, body_markdown)
                VALUES (?, '# Fixture body')
                """, id);
    }
}
