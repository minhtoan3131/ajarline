package vn.io.ajarline.topic.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.io.ajarline.support.TopicTestData.insertAssignment;
import static vn.io.ajarline.support.TopicTestData.insertContentItem;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
class TopicDatabaseConstraintIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void removeRows() {
        jdbcTemplate.update("DELETE FROM content_item");
        jdbcTemplate.update("DELETE FROM topic");
    }

    @Test
    void insert_ShouldRejectDuplicateSlug_WhenSlugAlreadyExists() {
        insertTopic(jdbcTemplate, uuid(1), "same-slug", "First", true);

        assertThatThrownBy(() -> insertTopic(jdbcTemplate, uuid(2), "same-slug", "Second", true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_topic_slug");
    }

    @Test
    void insert_ShouldAcceptDuplicateName_WhenSlugsDiffer() {
        insertTopic(jdbcTemplate, uuid(1), "first-slug", "Same name", true);

        insertTopic(jdbcTemplate, uuid(2), "second-slug", "Same name", true);

        assertThat(topicCount()).isEqualTo(2);
    }

    @Test
    void insert_ShouldRejectSlug_WhenSlugIsNotLowercase() {
        assertThatThrownBy(() -> insertTopic(jdbcTemplate, uuid(3), "Not-Lowercase", "Name", true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_topic_slug_lowercase");
    }

    @Test
    void insert_ShouldRejectSlug_WhenSlugIsBlank() {
        assertThatThrownBy(() -> insertTopic(jdbcTemplate, uuid(4), "   ", "Name", true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_topic_slug_not_blank");
    }

    @Test
    void insert_ShouldRejectSlug_WhenSlugExceedsDatabaseLimit() {
        String slug = "a".repeat(161);

        assertThatThrownBy(() -> insertTopic(jdbcTemplate, uuid(5), slug, "Name", true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("character varying(160)");
    }

    @Test
    void insert_ShouldRejectName_WhenNameExceedsDatabaseLimit() {
        String name = "a".repeat(201);

        assertThatThrownBy(() -> insertTopic(jdbcTemplate, uuid(6), "long-name", name, true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("character varying(200)");
    }

    @Test
    void insert_ShouldRejectRow_WhenNameIsNull() {
        assertThatThrownBy(() -> insertTopic(jdbcTemplate, uuid(7), "null-name", null, true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("name");
    }

    @Test
    void insert_ShouldRejectRow_WhenActiveIsNull() {
        assertThatThrownBy(() -> insertTopic(jdbcTemplate, uuid(8), "null-active", "Name", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("active");
    }

    @Test
    void assign_ShouldRejectRow_WhenTopicDoesNotExist() {
        insertContentItem(jdbcTemplate, uuid(10), "an-article");

        assertThatThrownBy(() -> insertAssignment(jdbcTemplate, uuid(10), uuid(99)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_content_topic_topic_id");
    }

    @Test
    void assign_ShouldRejectRow_WhenContentItemDoesNotExist() {
        insertTopic(jdbcTemplate, uuid(1), "a-topic", "Name", true);

        assertThatThrownBy(() -> insertAssignment(jdbcTemplate, uuid(99), uuid(1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_content_topic_content_id");
    }

    @Test
    void assign_ShouldRejectRow_WhenPairAlreadyExists() {
        insertTopic(jdbcTemplate, uuid(1), "a-topic", "Name", true);
        insertContentItem(jdbcTemplate, uuid(10), "an-article");
        insertAssignment(jdbcTemplate, uuid(10), uuid(1));

        assertThatThrownBy(() -> insertAssignment(jdbcTemplate, uuid(10), uuid(1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("pk_content_topic");
    }

    @Test
    void deleteTopic_ShouldBeRejected_WhenTopicIsAssigned() {
        insertTopic(jdbcTemplate, uuid(1), "a-topic", "Name", true);
        insertContentItem(jdbcTemplate, uuid(10), "an-article");
        insertAssignment(jdbcTemplate, uuid(10), uuid(1));

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM topic WHERE id = ?", uuid(1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_content_topic_topic_id");
        assertThat(topicCount()).isEqualTo(1);
    }

    @Test
    void deleteContentItem_ShouldRemoveAssignment_WhenContentItemIsDeleted() {
        insertTopic(jdbcTemplate, uuid(1), "a-topic", "Name", true);
        insertContentItem(jdbcTemplate, uuid(10), "an-article");
        insertAssignment(jdbcTemplate, uuid(10), uuid(1));

        jdbcTemplate.update("DELETE FROM content_item WHERE id = ?", uuid(10));

        assertThat(count("content_topic")).isZero();
        assertThat(topicCount()).isEqualTo(1);
    }

    private long topicCount() {
        return count("topic");
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
