package vn.io.ajarline.content.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.content.contract.ArticleContentInput;
import vn.io.ajarline.content.contract.ArticleCreator;
import vn.io.ajarline.content.contract.ArticleEditor;
import vn.io.ajarline.content.contract.ArticleReader;
import vn.io.ajarline.content.contract.ArticleView;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.MutableClock;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class ArticleTopicAssignmentIT {

    private static final Instant CREATED_AT = Instant.parse("2026-11-14T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-14T01:00:00Z");
    private static final UUID JAVA = uuid(1);
    private static final UUID SPRING = uuid(2);
    private static final UUID LEGACY = uuid(3);

    @Autowired
    private ArticleCreator creator;

    @Autowired
    private ArticleReader reader;

    @Autowired
    private ArticleEditor editor;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabaseAndClock() {
        jdbcTemplate.update("DELETE FROM content_item");
        jdbcTemplate.update("DELETE FROM topic");
        insertTopic(jdbcTemplate, JAVA, "java", "Java", true);
        insertTopic(jdbcTemplate, SPRING, "spring", "Spring", true);
        insertTopic(jdbcTemplate, LEGACY, "legacy", "Legacy", false);
        clock.set(CREATED_AT);
    }

    @Test
    void create_ShouldAssignTopicsAndKeepRevisionOne_WhenTopicsAreActive() {
        ArticleView created = creator.create(input("java-threads", "Body", SPRING, JAVA));

        assertThat(created.revision()).isEqualTo(1);
        assertThat(created.topicIds()).containsExactly(JAVA, SPRING);
        assertThat(assignedTopicIds(created.id())).containsExactly(JAVA, SPRING);
    }

    @Test
    void create_ShouldRollbackWholeArticle_WhenTopicIsInactive() {
        assertThatThrownBy(() -> creator.create(input("rolled-back", "Body", LEGACY)))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ContentFailureCode.INACTIVE_TOPIC_ASSIGNMENT));

        assertThat(rowCount("content_item")).isZero();
        assertThat(rowCount("article")).isZero();
    }

    @Test
    void create_ShouldRejectArticle_WhenTopicDoesNotExist() {
        assertThatThrownBy(() -> creator.create(input("unknown-topic", "Body", uuid(99))))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ContentFailureCode.TOPIC_REFERENCE_NOT_FOUND));

        assertThat(rowCount("content_item")).isZero();
    }

    @Test
    void replace_ShouldAdvanceRevisionOnce_WhenOnlyTopicsChange() {
        ArticleView created = creator.create(input("topics-only", "Body"));
        clock.set(LATER);

        ArticleView updated = editor.replace(created.id(), 1, input("topics-only", "Body", JAVA));

        assertThat(updated.revision()).isEqualTo(2);
        assertThat(updated.updatedAt()).isEqualTo(LATER);
        assertThat(updated.topicIds()).containsExactly(JAVA);
    }

    @Test
    void replace_ShouldKeepRevisionAndUpdatedAt_WhenTopicsAreUnchanged() {
        ArticleView created = creator.create(input("same-topics", "Body", JAVA));
        clock.set(LATER);

        ArticleView updated = editor.replace(created.id(), 1, input("same-topics", "Body", JAVA));

        assertThat(updated.revision()).isEqualTo(1);
        assertThat(updated.updatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void replace_ShouldAdvanceRevisionOnce_WhenContentAndTopicsBothChange() {
        ArticleView created = creator.create(input("both-change", "Old body"));

        ArticleView updated = editor.replace(
                created.id(), 1, input("both-change", "New body", JAVA, SPRING));

        assertThat(updated.revision()).isEqualTo(2);
        assertThat(updated.bodyMarkdown()).isEqualTo("New body");
        assertThat(updated.topicIds()).containsExactly(JAVA, SPRING);
    }

    @Test
    void replace_ShouldRemoveTopics_WhenListIsEmpty() {
        ArticleView created = creator.create(input("remove-all", "Body", JAVA, SPRING));

        ArticleView updated = editor.replace(created.id(), 1, input("remove-all", "Body"));

        assertThat(updated.revision()).isEqualTo(2);
        assertThat(updated.topicIds()).isEmpty();
        assertThat(assignedTopicIds(created.id())).isEmpty();
    }

    @Test
    void replace_ShouldKeepAssignedTopic_WhenTopicWasTurnedOffAfterAssignment() {
        ArticleView created = creator.create(input("kept-topic", "Body", JAVA));
        jdbcTemplate.update("UPDATE topic SET active = false WHERE id = ?", JAVA);

        ArticleView updated = editor.replace(created.id(), 1, input("kept-topic", "Changed body", JAVA));

        assertThat(updated.revision()).isEqualTo(2);
        assertThat(updated.topicIds()).containsExactly(JAVA);
    }

    @Test
    void replace_ShouldRejectAndKeepState_WhenInactiveTopicIsNewlyAdded() {
        ArticleView created = creator.create(input("rejected-topic", "Original body"));

        assertThatThrownBy(() -> editor.replace(
                created.id(), 1, input("rejected-topic", "Changed body", LEGACY)))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ContentFailureCode.INACTIVE_TOPIC_ASSIGNMENT));

        ArticleView stored = reader.get(created.id());
        assertThat(stored.bodyMarkdown()).isEqualTo("Original body");
        assertThat(stored.revision()).isEqualTo(1);
        assertThat(stored.topicIds()).isEmpty();
    }

    @Test
    void replace_ShouldRollbackTopics_WhenSlugConflicts() {
        creator.create(input("taken-slug", "Body"));
        ArticleView target = creator.create(input("other-slug", "Body"));

        assertThatThrownBy(() -> editor.replace(
                target.id(), 1, input("taken-slug", "Body", JAVA)))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ContentFailureCode.SLUG_CONFLICT));

        assertThat(assignedTopicIds(target.id())).isEmpty();
    }

    @Test
    void replace_ShouldRejectUnknownTopic_WhenIdDoesNotExist() {
        ArticleView created = creator.create(input("unknown-on-replace", "Body"));

        assertThatThrownBy(() -> editor.replace(
                created.id(), 1, input("unknown-on-replace", "Body", uuid(99))))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ContentFailureCode.TOPIC_REFERENCE_NOT_FOUND));
    }

    @Test
    void get_ShouldReturnTopicIdsOrderedById_WhenTopicsAreAssigned() {
        ArticleView created = creator.create(input("ordered-topics", "Body", SPRING, JAVA));

        ArticleView loaded = reader.get(created.id());

        assertThat(loaded.topicIds()).containsExactly(JAVA, SPRING);
    }

    private static ArticleContentInput input(String slug, String bodyMarkdown, UUID... topicIds) {
        return new ArticleContentInput("Java threads", slug, null, bodyMarkdown, List.of(topicIds));
    }

    private List<UUID> assignedTopicIds(UUID contentId) {
        return jdbcTemplate.queryForList(
                "SELECT topic_id FROM content_topic WHERE content_id = ? ORDER BY topic_id",
                UUID.class, contentId);
    }

    private long rowCount(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
