package vn.io.ajarline.topic.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.content.contract.TopicLabel;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.ContentTestData.insertArticle;
import static vn.io.ajarline.support.TopicTestData.insertAssignment;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class PublicTopicApplicationIT {

    private static final UUID JAVA = uuid(1);
    private static final UUID SPRING = uuid(2);
    private static final UUID HIDDEN = uuid(3);
    private static final UUID UNUSED = uuid(4);
    private static final UUID JAVA_CORE = uuid(5);
    private static final UUID ALPHA = uuid(101);
    private static final UUID BETA = uuid(102);
    private static final UUID DRAFT_ID = uuid(103);
    private static final UUID UNPUBLISHED_ID = uuid(104);
    private static final Set<ContentType> ALL_TYPES = EnumSet.allOf(ContentType.class);

    @Autowired
    private PublicTopicService publicTopics;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("DELETE FROM content_item");
        jdbcTemplate.update("DELETE FROM topic");

        insertTopic(jdbcTemplate, JAVA, "java", "Java", true);
        // Same name as JAVA, larger id: the id decides the order.
        insertTopic(jdbcTemplate, JAVA_CORE, "java-core", "Java", true);
        // Inactive, but carried by a public Article.
        insertTopic(jdbcTemplate, SPRING, "spring", "Spring", false);
        // Carried only by Draft and Unpublished Articles.
        insertTopic(jdbcTemplate, HIDDEN, "hidden", "Hidden", true);
        insertTopic(jdbcTemplate, UNUSED, "unused", "Unused", true);

        insertArticle(jdbcTemplate, BETA, "beta", "Beta", PublicationStatus.PUBLISHED);
        insertArticle(jdbcTemplate, ALPHA, "alpha", "Alpha", PublicationStatus.PUBLISHED);
        insertArticle(jdbcTemplate, DRAFT_ID, "draft", "Draft", PublicationStatus.DRAFT);
        insertArticle(jdbcTemplate, UNPUBLISHED_ID, "unpublished", "Unpublished",
                PublicationStatus.UNPUBLISHED);

        insertAssignment(jdbcTemplate, ALPHA, JAVA);
        insertAssignment(jdbcTemplate, ALPHA, SPRING);
        insertAssignment(jdbcTemplate, BETA, JAVA);
        insertAssignment(jdbcTemplate, BETA, JAVA_CORE);
        insertAssignment(jdbcTemplate, DRAFT_ID, JAVA);
        insertAssignment(jdbcTemplate, DRAFT_ID, HIDDEN);
        insertAssignment(jdbcTemplate, UNPUBLISHED_ID, HIDDEN);
    }

    @Test
    void listPublicTopics_ShouldReturnTopicsWithPublicContent_OrderedByNameThenId() {
        assertThat(publicTopics.listPublicTopics())
                .extracting(TopicLabel::id)
                .containsExactly(JAVA, JAVA_CORE, SPRING);
    }

    @Test
    void findPublicTopic_ShouldReturnTopic_WhenTopicIsInactiveButHasPublicContent() {
        PublicTopicView view = publicTopics.findPublicTopic("spring").orElseThrow();

        assertThat(view.id()).isEqualTo(SPRING);
        assertThat(view.slug()).isEqualTo("spring");
        assertThat(view.name()).isEqualTo("Spring");
        assertThat(view.description()).isNull();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"hidden", "unused", "missing"})
    void findPublicTopic_ShouldBeEmpty_WhenTopicHasNoPublicContent(String slug) {
        assertThat(publicTopics.findPublicTopic(slug)).isEmpty();
    }

    @Test
    void findPublicContent_ShouldListPublicItemsWithAllTheirTopics_WhenTopicIsPublic() {
        PublicTopicContent content = publicTopics
                .findPublicContent("java", ALL_TYPES, 0, 20)
                .orElseThrow();

        assertThat(content.items())
                .extracting(entry -> entry.content().id())
                .containsExactly(ALPHA, BETA);
        assertThat(content.items().get(0).topics())
                .extracting(TopicLabel::id)
                .containsExactly(JAVA, SPRING);
        assertThat(content.items().get(1).topics())
                .extracting(TopicLabel::id)
                .containsExactly(JAVA, JAVA_CORE);
        assertThat(content.hasMore()).isFalse();
    }

    @Test
    void findPublicContent_ShouldPageAfterOrdering_WhenSizeIsOne() {
        PublicTopicContent first = publicTopics.findPublicContent("java", ALL_TYPES, 0, 1).orElseThrow();
        PublicTopicContent second = publicTopics.findPublicContent("java", ALL_TYPES, 1, 1).orElseThrow();

        assertThat(first.items()).extracting(entry -> entry.content().id()).containsExactly(ALPHA);
        assertThat(first.hasMore()).isTrue();
        assertThat(second.items()).extracting(entry -> entry.content().id()).containsExactly(BETA);
        assertThat(second.hasMore()).isFalse();
    }

    @Test
    void findPublicContent_ShouldBeEmpty_WhenTopicIsNotPublic() {
        assertThat(publicTopics.findPublicContent("hidden", ALL_TYPES, 0, 20)).isEmpty();
    }

    @Test
    void findPublicContent_ShouldReturnNoItems_WhenNoTypeIsKept() {
        PublicTopicContent content = publicTopics
                .findPublicContent("java", Set.of(), 0, 20)
                .orElseThrow();

        assertThat(content.items()).isEmpty();
        assertThat(content.hasMore()).isFalse();
    }
}
