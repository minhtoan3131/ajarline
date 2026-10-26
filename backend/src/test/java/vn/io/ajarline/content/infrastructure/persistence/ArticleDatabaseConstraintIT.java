package vn.io.ajarline.content.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
public class ArticleDatabaseConstraintIT {

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.parse("2026-10-25T00:00:00Z");
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void removeCanonicalRows() {
        jdbcTemplate.update("DELETE FROM content_item");
    }

    @Test
    void insert_ShouldRejectDuplicateSlug_WhenTypeIsTheSame() {
        insertContentItem(fixedUuid(1), "same-slug", "Valid title", "DRAFT", 1);

        assertThatThrownBy(() -> insertContentItem(
                fixedUuid(2), "same-slug", "Another title", "DRAFT", 1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_content_item_type_slug");
    }

    @Test
    void insert_ShouldRejectType_WhenSubtypeIsNotActive() {
        UUID uuid = fixedUuid(3);
        assertThatThrownBy(() -> insertContentItem(uuid, "future-news", "Future News", "DRAFT",
                1, "NEWS"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_content_item_type");
    }

    @Test
    void insert_ShouldRejectStatus_WhenStatusIsUnknow() {
        UUID uuid = fixedUuid(4);
        assertThatThrownBy(() -> insertContentItem(uuid, "unknown-status", "Unknown status",
                "UNKNOWN-STATUS", 1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_content_item_status");
    }

    @Test
    void insert_ShouldRejectRevision_WhenRevisionIsBelowOne() {
        UUID uuid = fixedUuid(5);
        assertThatThrownBy(() -> insertContentItem(uuid, "revision-below-one", "Revision is below one",
                "DRAFT", 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_content_item_revision");
    }

    @Test
    void insert_ShouldRejectSlug_WhenSlugIsNotLowercase() {
        UUID uuid = fixedUuid(6);
        assertThatThrownBy(() -> insertContentItem(uuid, "Slug-is-not-lowercase", "Slug is not lowercase",
                "DRAFT", 1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_content_item_slug_lowercase");
    }

    @Test
    void insert_ShouldRejectTitle_WhenTitleExceedsDatabaseLimit() {
        UUID uuid = fixedUuid(7);
        String title = "a".repeat(301);
        assertThatThrownBy(() -> insertContentItem(uuid, "title-exceeds-database-limit", title, "DRAFT", 1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("character varying(300)");
    }

    @Test
    void insert_ShouldRejectTitle_WhenTitleIsNull() {
        UUID uuid = fixedUuid(8);
        assertThatThrownBy(() -> insertContentItem(uuid, "title-is-null", null, "DRAFT", 1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("title");
    }

    @Test
    void insert_ShouldRejectArticle_WhenContentItemDoesNotExist() {
        UUID missingContentId = fixedUuid(99);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO article (content_id, body_markdown)
                VALUES (?, ?)
                """, missingContentId, "# Title"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_article_content_id");
    }


    private void insertContentItem(
            UUID id,
            String slug,
            String title,
            String status,
            long revision
    ) {
        insertContentItem(id, slug, title, status, revision, "ARTICLE");
    }

    private void insertContentItem(UUID id, String slug, String title, String status, long revision, String type) {
        jdbcTemplate.update("""
                INSERT INTO content_item(id, type, slug, title, summary, status, revision, published_at, unpublished_at,
                created_at, updated_at)
                VALUES(?, ?, ?, ?, NULL, ?, ?, NULL, NULL, ?, ?)
                """, id, type, slug, title, status, revision, FIXED_TIME, FIXED_TIME);
    }

    private static UUID fixedUuid(int suffix) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(suffix));
    }
}
