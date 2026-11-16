package vn.io.ajarline.topic.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.content.contract.TopicAssignmentPort;
import vn.io.ajarline.content.contract.TopicLabel;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.io.ajarline.support.TopicTestData.insertAssignment;
import static vn.io.ajarline.support.TopicTestData.insertContentItem;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class TopicAssignmentApplicationIT {

    private static final UUID CONTENT_ID = uuid(100);
    private static final UUID JAVA = uuid(1);
    private static final UUID SPRING = uuid(2);
    private static final UUID LEGACY = uuid(3);
    private static final UUID OTHER_CONTENT_ID = uuid(101);
    private static final UUID EMPTY_CONTENT_ID = uuid(102);
    private static final UUID JAVA_TWIN = uuid(4);

    @Autowired
    private TopicAssignmentPort assignments;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactions;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("DELETE FROM content_item");
        jdbcTemplate.update("DELETE FROM topic");
        insertContentItem(jdbcTemplate, CONTENT_ID, "assigned-article");
        insertContentItem(jdbcTemplate, OTHER_CONTENT_ID, "other-article");
        insertContentItem(jdbcTemplate, EMPTY_CONTENT_ID, "untagged-article");
        insertTopic(jdbcTemplate, JAVA, "java", "Java", true);
        insertTopic(jdbcTemplate, SPRING, "spring", "Spring", true);
        insertTopic(jdbcTemplate, LEGACY, "legacy", "Legacy", false);
        insertTopic(jdbcTemplate, JAVA_TWIN, "java-twin", "Java", true);
    }

    @Test
    void replaceAssignments_ShouldAddTopicsAndReportChange_WhenTopicsAreActive() {
        boolean changed = replace(JAVA, SPRING);

        assertThat(changed).isTrue();
        assertThat(assignedTopicIds()).containsExactly(JAVA, SPRING);
    }

    @Test
    void replaceAssignments_ShouldReportNoChange_WhenSetIsUnchanged() {
        insertAssignment(jdbcTemplate, CONTENT_ID, JAVA);

        boolean changed = replace(JAVA);

        assertThat(changed).isFalse();
        assertThat(assignedTopicIds()).containsExactly(JAVA);
    }

    @Test
    void replaceAssignments_ShouldRemoveMissingTopics_WhenTopicIsInactiveToo() {
        insertAssignment(jdbcTemplate, CONTENT_ID, JAVA);
        insertAssignment(jdbcTemplate, CONTENT_ID, LEGACY);

        boolean changed = replace(JAVA);

        assertThat(changed).isTrue();
        assertThat(assignedTopicIds()).containsExactly(JAVA);
    }

    @Test
    void replaceAssignments_ShouldKeepAssignedInactiveTopic_WhenItStaysInTheSet() {
        insertAssignment(jdbcTemplate, CONTENT_ID, LEGACY);

        boolean unchanged = replace(LEGACY);
        boolean extended = replace(LEGACY, JAVA);

        assertThat(unchanged).isFalse();
        assertThat(extended).isTrue();
        assertThat(assignedTopicIds()).containsExactly(JAVA, LEGACY);
    }

    @Test
    void replaceAssignments_ShouldRejectInactiveTopic_WhenItIsNewlyAdded() {
        assertThatThrownBy(() -> replace(JAVA, LEGACY))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ContentFailureCode.INACTIVE_TOPIC_ASSIGNMENT));

        // The active Topic in the same request is not assigned either.
        assertThat(assignedTopicIds()).isEmpty();
    }

    @Test
    void replaceAssignments_ShouldRejectUnknownTopic_WhenIdDoesNotExist() {
        assertThatThrownBy(() -> replace(JAVA, uuid(99)))
                .isInstanceOfSatisfying(ContentOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ContentFailureCode.TOPIC_REFERENCE_NOT_FOUND));

        assertThat(assignedTopicIds()).isEmpty();
    }

    @Test
    void topicIdsOf_ShouldReturnIdsOrderedById_WhenTopicsAreAssigned() {
        insertAssignment(jdbcTemplate, CONTENT_ID, SPRING);
        insertAssignment(jdbcTemplate, CONTENT_ID, JAVA);

        List<UUID> ids = transactions.execute(status -> assignments.topicIdsOf(CONTENT_ID));

        assertThat(ids).containsExactly(JAVA, SPRING);
    }

    @Test
    void topicLabelsOf_ShouldGroupLabelsByContent_WhenSeveralItemsAreAsked() {
        insertAssignment(jdbcTemplate, CONTENT_ID, JAVA);
        insertAssignment(jdbcTemplate, CONTENT_ID, SPRING);
        insertAssignment(jdbcTemplate, OTHER_CONTENT_ID, SPRING);

        Map<UUID, List<TopicLabel>> labels = labelsOf(CONTENT_ID, OTHER_CONTENT_ID);

        assertThat(labels).containsOnlyKeys(CONTENT_ID, OTHER_CONTENT_ID);
        assertThat(labels.get(CONTENT_ID)).extracting(TopicLabel::slug)
                .containsExactly("java", "spring");
        assertThat(labels.get(OTHER_CONTENT_ID)).extracting(TopicLabel::slug)
                .containsExactly("spring");
    }

    @Test
    void topicLabelsOf_ShouldOrderLabelsByNameThenId_WhenTopicsShareAName() {
        insertAssignment(jdbcTemplate, CONTENT_ID, SPRING);
        insertAssignment(jdbcTemplate, CONTENT_ID, JAVA_TWIN);
        insertAssignment(jdbcTemplate, CONTENT_ID, JAVA);

        Map<UUID, List<TopicLabel>> labels = labelsOf(CONTENT_ID);

        assertThat(labels.get(CONTENT_ID)).extracting(TopicLabel::id)
                .containsExactly(JAVA, JAVA_TWIN, SPRING);
    }

    @Test
    void topicLabelsOf_ShouldHaveNoKey_WhenAnItemHasNoTopic() {
        insertAssignment(jdbcTemplate, CONTENT_ID, JAVA);

        Map<UUID, List<TopicLabel>> labels = labelsOf(CONTENT_ID, EMPTY_CONTENT_ID);

        assertThat(labels).containsOnlyKeys(CONTENT_ID);
    }

    @Test
    void topicLabelsOf_ShouldLeaveOutItems_WhenTheyWereNotAsked() {
        insertAssignment(jdbcTemplate, CONTENT_ID, JAVA);
        insertAssignment(jdbcTemplate, OTHER_CONTENT_ID, SPRING);

        Map<UUID, List<TopicLabel>> labels = labelsOf(CONTENT_ID);

        assertThat(labels).containsOnlyKeys(CONTENT_ID);
    }

    @Test
    void topicLabelsOf_ShouldIncludeInactiveTopic_WhenItIsAssigned() {
        insertAssignment(jdbcTemplate, CONTENT_ID, LEGACY);

        Map<UUID, List<TopicLabel>> labels = labelsOf(CONTENT_ID);

        assertThat(labels.get(CONTENT_ID)).extracting(TopicLabel::slug)
                .containsExactly("legacy");
    }

    @Test
    void topicLabelsOf_ShouldReturnEmptyMap_WhenNoIdIsGiven() {
        insertAssignment(jdbcTemplate, CONTENT_ID, JAVA);

        Map<UUID, List<TopicLabel>> labels = labelsOf();

        assertThat(labels).isEmpty();
    }

    private boolean replace(UUID... topicIds) {
        return Boolean.TRUE.equals(transactions.execute(
                status -> assignments.replaceAssignments(CONTENT_ID, List.of(topicIds))));
    }

    private Map<UUID, List<TopicLabel>> labelsOf(UUID... contentIds) {
        return transactions.execute(status -> assignments.topicLabelsOf(List.of(contentIds)));
    }

    private List<UUID> assignedTopicIds() {
        return jdbcTemplate.queryForList(
                "SELECT topic_id FROM content_topic WHERE content_id = ? ORDER BY topic_id",
                UUID.class, CONTENT_ID);
    }
}
