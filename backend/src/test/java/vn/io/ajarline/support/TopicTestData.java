package vn.io.ajarline.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Inserts rows directly through SQL so tests can build database states
 * that the application services would refuse to create.
 */
public final class TopicTestData {

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.parse("2026-11-12T00:00:00Z");

    private TopicTestData() {
    }

    public static UUID uuid(int suffix) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(suffix));
    }

    public static void insertTopic(
            JdbcTemplate jdbcTemplate, UUID id, String slug, String name, Boolean active) {
        jdbcTemplate.update("""
                INSERT INTO topic(id, slug, name, description, active, created_at, updated_at)
                VALUES (?, ?, ?, NULL, ?, ?, ?)
                """, id, slug, name, active, FIXED_TIME, FIXED_TIME);
    }

    public static void insertContentItem(JdbcTemplate jdbcTemplate, UUID id, String slug) {
        jdbcTemplate.update("""
                INSERT INTO content_item(id, type, slug, title, summary, status, revision,
                    published_at, unpublished_at, created_at, updated_at)
                VALUES (?, 'ARTICLE', ?, 'Fixture title', NULL, 'DRAFT', 1, NULL, NULL, ?, ?)
                """, id, slug, FIXED_TIME, FIXED_TIME);
    }

    public static void insertAssignment(JdbcTemplate jdbcTemplate, UUID contentId, UUID topicId) {
        jdbcTemplate.update("""
                INSERT INTO content_topic(content_id, topic_id)
                VALUES (?, ?)
                """, contentId, topicId);
    }
}
