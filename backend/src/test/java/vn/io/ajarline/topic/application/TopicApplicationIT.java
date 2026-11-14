package vn.io.ajarline.topic.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.MutableClock;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;
import vn.io.ajarline.topic.contract.TopicCreateInput;
import vn.io.ajarline.topic.contract.TopicCreator;
import vn.io.ajarline.topic.contract.TopicEditor;
import vn.io.ajarline.topic.contract.TopicFailureCode;
import vn.io.ajarline.topic.contract.TopicOperationException;
import vn.io.ajarline.topic.contract.TopicReader;
import vn.io.ajarline.topic.contract.TopicUpdateInput;
import vn.io.ajarline.topic.contract.TopicView;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
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
class TopicApplicationIT {

    private static final Instant CREATED_AT = Instant.parse("2026-11-12T00:00:00Z");

    @Autowired
    private TopicCreator creator;

    @Autowired
    private TopicReader reader;

    @Autowired
    private TopicEditor editor;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabaseAndClock() {
        jdbcTemplate.update("DELETE FROM content_item");
        jdbcTemplate.update("DELETE FROM topic");
        clock.set(CREATED_AT);
    }

    @Test
    void create_ShouldReturnTopic_WhenInputIsValid() {
        TopicView created = creator.create(
                new TopicCreateInput(" Java-Basics ", "Java basics", "Core Java.", true));

        assertThat(created.id()).isNotNull();
        assertThat(created.slug()).isEqualTo("java-basics");
        assertThat(created.name()).isEqualTo("Java basics");
        assertThat(created.description()).isEqualTo("Core Java.");
        assertThat(created.active()).isTrue();
        assertThat(created.createdAt()).isEqualTo(CREATED_AT);
        assertThat(created.updatedAt()).isEqualTo(CREATED_AT);
        assertThat(topicCount()).isEqualTo(1);
    }

    @Test
    void create_ShouldStoreNullDescription_WhenDescriptionIsEmpty() {
        TopicView created = creator.create(new TopicCreateInput("empty-description", "Name", "", true));

        assertThat(created.description()).isNull();
    }

    @Test
    void create_ShouldRejectTopic_WhenSlugAlreadyExists() {
        creator.create(new TopicCreateInput("same-slug", "First", null, true));

        assertThatThrownBy(() -> creator.create(new TopicCreateInput("Same-Slug", "Second", null, true)))
                .isInstanceOfSatisfying(TopicOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(TopicFailureCode.SLUG_CONFLICT));
        assertThat(topicCount()).isEqualTo(1);
    }

    @Test
    void create_ShouldRejectTopic_WhenInputIsInvalid() {
        assertThatThrownBy(() -> creator.create(new TopicCreateInput("Not a slug", "Name", null, true)))
                .isInstanceOfSatisfying(TopicOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(TopicFailureCode.VALIDATION_FAILED));
        assertThat(topicCount()).isZero();
    }

    @Test
    void list_ShouldReturnEmptyList_WhenNoTopicExists() {
        assertThat(reader.list()).isEmpty();
    }

    @Test
    void list_ShouldReturnActiveAndInactiveTopicsOrderedByName_WhenTopicsExist() {
        creator.create(new TopicCreateInput("gamma", "Gamma", null, true));
        creator.create(new TopicCreateInput("alpha", "Alpha", null, false));
        creator.create(new TopicCreateInput("beta", "Beta", null, true));

        assertThat(reader.list())
                .extracting(TopicView::name)
                .containsExactly("Alpha", "Beta", "Gamma");
        assertThat(reader.list())
                .extracting(TopicView::active)
                .containsExactly(false, true, true);
    }

    @Test
    void list_ShouldOrderById_WhenNamesAreEqual() {
        insertTopic(jdbcTemplate, uuid(2), "second-slug", "Same name", true);
        insertTopic(jdbcTemplate, uuid(1), "first-slug", "Same name", true);

        assertThat(reader.list())
                .extracting(TopicView::id)
                .containsExactly(uuid(1), uuid(2));
    }

    @Test
    void replace_ShouldChangeEditableValuesAndUpdatedAt_WhenValuesDiffer() {
        TopicView created = creator.create(new TopicCreateInput("java-basics", "Java", null, true));
        clock.set(CREATED_AT.plus(Duration.ofHours(1)));

        TopicView updated = editor.replace(
                created.id(), new TopicUpdateInput("Java basics", "Core Java.", false));

        assertThat(updated.slug()).isEqualTo("java-basics");
        assertThat(updated.name()).isEqualTo("Java basics");
        assertThat(updated.description()).isEqualTo("Core Java.");
        assertThat(updated.active()).isFalse();
        assertThat(updated.createdAt()).isEqualTo(CREATED_AT);
        assertThat(updated.updatedAt()).isEqualTo(CREATED_AT.plus(Duration.ofHours(1)));
        assertThat(storedUpdatedAt(created.id())).isEqualTo(updated.updatedAt());
    }

    @Test
    void replace_ShouldKeepUpdatedAt_WhenNoValueDiffers() {
        TopicView created = creator.create(new TopicCreateInput("java-basics", "Java", "Core.", true));
        clock.set(CREATED_AT.plus(Duration.ofHours(1)));

        TopicView result = editor.replace(
                created.id(), new TopicUpdateInput("Java", "Core.", true));

        assertThat(result.updatedAt()).isEqualTo(CREATED_AT);
        assertThat(storedUpdatedAt(created.id())).isEqualTo(CREATED_AT);
    }

    @Test
    void replace_ShouldTreatEmptyDescriptionAsNull_WhenStoredDescriptionIsNull() {
        TopicView created = creator.create(new TopicCreateInput("java-basics", "Java", null, true));
        clock.set(CREATED_AT.plus(Duration.ofHours(1)));

        TopicView result = editor.replace(created.id(), new TopicUpdateInput("Java", "", true));

        assertThat(result.updatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void replace_ShouldRejectUpdate_WhenTopicDoesNotExist() {
        assertThatThrownBy(() -> editor.replace(
                uuid(404), new TopicUpdateInput("Name", null, true)))
                .isInstanceOfSatisfying(TopicOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(TopicFailureCode.TOPIC_NOT_FOUND));
    }

    @Test
    void replace_ShouldRejectUpdate_WhenValuesAreInvalid() {
        TopicView created = creator.create(new TopicCreateInput("java-basics", "Java", null, true));

        assertThatThrownBy(() -> editor.replace(created.id(), new TopicUpdateInput("  ", null, true)))
                .isInstanceOfSatisfying(TopicOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(TopicFailureCode.VALIDATION_FAILED));
    }

    @Test
    void replace_ShouldReportValidationFailure_WhenValuesAreInvalidAndTopicDoesNotExist() {
        assertThatThrownBy(() -> editor.replace(uuid(404), new TopicUpdateInput("  ", null, true)))
                .isInstanceOfSatisfying(TopicOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(TopicFailureCode.VALIDATION_FAILED));
    }

    private Instant storedUpdatedAt(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT updated_at FROM topic WHERE id = ?", OffsetDateTime.class, id).toInstant();
    }

    private long topicCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM topic", Long.class);
    }
}
