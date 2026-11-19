package vn.io.ajarline.support;

import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.content.contract.PublicationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Inserts News with SQL, so tests can set any state without the News API or the publication
 * workflow. The title comes from the slug.
 */
public final class NewsTestData {

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.parse("2026-11-19T00:00:00Z");

    private NewsTestData() {
    }

    /**
     * Inserts a draft with a summary, a body and a complete valid source.
     */
    public static void insertCompleteDraft(JdbcTemplate jdbcTemplate, UUID id, String slug) {
        insertNews(jdbcTemplate, id, slug, PublicationStatus.DRAFT,
                "Fixture summary", "# Fixture body", "Fixture source", "https://example.com/story");
    }

    /**
     * Inserts News with the given parts. A published or unpublished item gets a publication
     * time, as the publication workflow would set. The revision is 1.
     */
    public static void insertNews(
            JdbcTemplate jdbcTemplate, UUID id, String slug, PublicationStatus status,
            String summary, String bodyMarkdown, String sourceName, String sourceUrl) {
        OffsetDateTime publishedAt = status == PublicationStatus.DRAFT ? null : FIXED_TIME;
        OffsetDateTime unpublishedAt = status == PublicationStatus.UNPUBLISHED ? FIXED_TIME : null;
        jdbcTemplate.update("""
                INSERT INTO content_item(id, type, slug, title, summary, status, revision,
                    published_at, unpublished_at, created_at, updated_at)
                VALUES (?, 'NEWS', ?, ?, ?, ?, 1, ?, ?, ?, ?)
                """, id, slug, "News " + slug, summary, status.name(),
                publishedAt, unpublishedAt, FIXED_TIME, FIXED_TIME);
        jdbcTemplate.update("""
                INSERT INTO news(content_id, body_markdown, source_name, source_url, source_published_at)
                VALUES (?, ?, ?, ?, NULL)
                """, id, bodyMarkdown, sourceName, sourceUrl);
    }
}
