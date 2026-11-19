package vn.io.ajarline.content.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class NewsDatabaseConstraintIT {

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.parse("2026-11-19T00:00:00Z");
    private static final UUID NEWS_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void removeCanonicalRows() {
        jdbcTemplate.update("DELETE FROM content_item");
    }

    @ParameterizedTest(name = "[{index}] rejects {0}")
    @ValueSource(strings = {
            "javascript:alert(1)", "JAVASCRIPT:alert(1)", "data:text/html,x",
            "ftp://example.com", "//example.com", "example.com"})
    void insert_ShouldRejectSourceUrl_WhenSchemeIsNotHttp(String url) {
        insertNewsItem("DRAFT", "A summary");

        assertThatThrownBy(() -> insertNewsRow(NEWS_ID, "A source", url))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_news_source_url_scheme");
    }

    @ParameterizedTest(name = "[{index}] accepts {0}")
    @ValueSource(strings = {"http://example.com", "https://example.com/a?b=1", "HTTPS://EXAMPLE.COM"})
    void insert_ShouldAcceptSourceUrl_WhenSchemeIsHttpOrHttps(String url) {
        insertNewsItem("DRAFT", "A summary");

        insertNewsRow(NEWS_ID, "A source", url);

        assertThat(newsRowCount()).isEqualTo(1L);
    }

    @Test
    void insert_ShouldAcceptRow_WhenSourceIsEmptyInADraft() {
        insertNewsItem("DRAFT", null);

        insertNewsRow(NEWS_ID, null, null);

        assertThat(newsRowCount()).isEqualTo(1L);
    }

    @Test
    void insert_ShouldRejectSourceName_WhenNameExceedsDatabaseLimit() {
        insertNewsItem("DRAFT", null);

        assertThatThrownBy(() -> insertNewsRow(NEWS_ID, "a".repeat(301), null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("character varying(300)");
    }

    @Test
    void insert_ShouldRejectSourceUrl_WhenUrlExceedsDatabaseLimit() {
        insertNewsItem("DRAFT", null);

        assertThatThrownBy(() -> insertNewsRow(NEWS_ID, null, "https://example.com/" + "a".repeat(2048)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("character varying(2048)");
    }

    @Test
    void insert_ShouldRejectNews_WhenContentItemDoesNotExist() {
        assertThatThrownBy(() -> insertNewsRow(NEWS_ID, null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_news_content_id");
    }

    @Test
    void insert_ShouldRejectPublishedNews_WhenSummaryIsNull() {
        assertThatThrownBy(() -> insertNewsItem("PUBLISHED", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_content_item_published_summary");
    }

    @Test
    void delete_ShouldRemoveNewsRow_WhenContentItemIsDeleted() {
        insertNewsItem("DRAFT", null);
        insertNewsRow(NEWS_ID, null, null);

        jdbcTemplate.update("DELETE FROM content_item WHERE id = ?", NEWS_ID);

        assertThat(newsRowCount()).isZero();
    }

    private void insertNewsItem(String status, String summary) {
        jdbcTemplate.update("""
                INSERT INTO content_item(id, type, slug, title, summary, status, revision,
                    published_at, unpublished_at, created_at, updated_at)
                VALUES (?, 'NEWS', 'a-news-item', 'A news item', ?, ?, 1, NULL, NULL, ?, ?)
                """, NEWS_ID, summary, status, FIXED_TIME, FIXED_TIME);
    }

    private void insertNewsRow(UUID contentId, String sourceName, String sourceUrl) {
        jdbcTemplate.update("""
                INSERT INTO news(content_id, body_markdown, source_name, source_url, source_published_at)
                VALUES (?, '# Body', ?, ?, NULL)
                """, contentId, sourceName, sourceUrl);
    }

    private long newsRowCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM news", Long.class);
    }
}
