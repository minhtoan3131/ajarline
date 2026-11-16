package vn.io.ajarline.content.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicContentPage;
import vn.io.ajarline.content.contract.PublicContentQuery;
import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.content.contract.TopicAssignmentPort;
import vn.io.ajarline.content.contract.TopicLabel;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static vn.io.ajarline.support.ContentTestData.insertArticle;
import static vn.io.ajarline.support.TopicTestData.insertAssignment;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class PublicContentQueryIT {

    private static final UUID ALPHA = uuid(101);
    private static final UUID BETA_FIRST = uuid(102);
    private static final UUID BETA_SECOND = uuid(103);
    private static final UUID DRAFT_ID = uuid(104);
    private static final UUID UNPUBLISHED_ID = uuid(105);
    private static final UUID SPRING = uuid(1);
    private static final UUID JAVA = uuid(2);
    private static final Set<ContentType> ALL_TYPES = EnumSet.allOf(ContentType.class);

    @Autowired
    private PublicContentQuery publicContent;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PublicContentService standaloneArticles;

    @MockitoSpyBean
    private TopicAssignmentPort topicAssignments;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("DELETE FROM content_item");
        jdbcTemplate.update("DELETE FROM topic");
        // Inserted out of order so the test proves the query sorts.
        insertArticle(jdbcTemplate, BETA_SECOND, "beta-second", "Beta", PublicationStatus.PUBLISHED);
        insertArticle(jdbcTemplate, ALPHA, "alpha", "Alpha", PublicationStatus.PUBLISHED);
        insertArticle(jdbcTemplate, BETA_FIRST, "beta-first", "Beta", PublicationStatus.PUBLISHED);
        insertArticle(jdbcTemplate, DRAFT_ID, "draft", "Draft", PublicationStatus.DRAFT);
        insertArticle(jdbcTemplate, UNPUBLISHED_ID, "unpublished", "Unpublished", PublicationStatus.UNPUBLISHED);
        insertTopic(jdbcTemplate, SPRING, "spring", "Spring", true);
        insertTopic(jdbcTemplate, JAVA, "java", "Java", true);
        insertAssignment(jdbcTemplate, ALPHA, SPRING);
        insertAssignment(jdbcTemplate, ALPHA, JAVA);
        insertAssignment(jdbcTemplate, DRAFT_ID, JAVA);
    }

    @Test
    void findPublicIds_ShouldKeepOnlyPublishedItems_WhenDraftAndUnpublishedAreGiven() {
        Set<UUID> ids = publicContent.findPublicIds(
                List.of(ALPHA, DRAFT_ID, UNPUBLISHED_ID, uuid(999)));

        assertThat(ids).containsExactly(ALPHA);
    }

    @Test
    void findPublicPage_ShouldOrderByTitleThenId_WhenItemsAreArticles() {
        PublicContentPage page = publicContent.findPublicPage(allIds(), ALL_TYPES, 0, 20);

        assertThat(ids(page)).containsExactly(ALPHA, BETA_FIRST, BETA_SECOND);
        assertThat(page.hasMore()).isFalse();
        PublicContentSummary first = page.items().getFirst();
        assertThat(first.type()).isEqualTo(ContentType.ARTICLE);
        assertThat(first.slug()).isEqualTo("alpha");
        assertThat(first.title()).isEqualTo("Alpha");
        assertThat(first.summary()).isEqualTo("Fixture summary");
        assertThat(first.publishedAt()).isNotNull();
        assertThat(first.updatedAt()).isNotNull();
    }

    @Test
    void findPublicPage_ShouldPageAfterOrdering_WhenSizeIsSmall() {
        PublicContentPage first = publicContent.findPublicPage(allIds(), ALL_TYPES, 0, 2);
        PublicContentPage second = publicContent.findPublicPage(allIds(), ALL_TYPES, 1, 2);
        PublicContentPage afterLast = publicContent.findPublicPage(allIds(), ALL_TYPES, 2, 2);

        assertThat(ids(first)).containsExactly(ALPHA, BETA_FIRST);
        assertThat(first.hasMore()).isTrue();
        assertThat(ids(second)).containsExactly(BETA_SECOND);
        assertThat(second.hasMore()).isFalse();
        assertThat(afterLast.items()).isEmpty();
        assertThat(afterLast.hasMore()).isFalse();
    }

    @Test
    void findPublicPage_ShouldReadOnlyGivenIds_WhenOtherPublicItemsExist() {
        PublicContentPage page = publicContent.findPublicPage(
                List.of(BETA_SECOND, DRAFT_ID), ALL_TYPES, 0, 20);

        assertThat(ids(page)).containsExactly(BETA_SECOND);
    }

    @Test
    void findPublicPage_ShouldReturnNothing_WhenNoTypeIsKept() {
        PublicContentPage page = publicContent.findPublicPage(allIds(), Set.of(), 0, 20);

        assertThat(page.items()).isEmpty();
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void findStandaloneArticles_ShouldReturnOnlyPublishedOrderedByTitleThenId_WhenOtherStatesExist() {
        StandaloneArticlePage page = standaloneArticles.findStandaloneArticles(0, 20);

        assertThat(contentIds(page)).containsExactly(ALPHA, BETA_FIRST, BETA_SECOND);
        assertThat(page.hasMore()).isFalse();
        PublicContentSummary first = page.entries().getFirst().content();
        assertThat(first.type()).isEqualTo(ContentType.ARTICLE);
        assertThat(first.slug()).isEqualTo("alpha");
        assertThat(first.summary()).isEqualTo("Fixture summary");
        assertThat(first.publishedAt()).isNotNull();
    }

    @Test
    void findStandaloneArticles_ShouldReportHasMoreOnlyWhenAnotherArticleExists_WhenSizeChanges() {
        StandaloneArticlePage exactFit = standaloneArticles.findStandaloneArticles(0, 3);
        StandaloneArticlePage oneShort = standaloneArticles.findStandaloneArticles(0, 2);
        StandaloneArticlePage secondPage = standaloneArticles.findStandaloneArticles(1, 2);

        assertThat(contentIds(exactFit)).hasSize(3);
        assertThat(exactFit.hasMore()).isFalse();
        assertThat(contentIds(oneShort)).containsExactly(ALPHA, BETA_FIRST);
        assertThat(oneShort.hasMore()).isTrue();
        assertThat(contentIds(secondPage)).containsExactly(BETA_SECOND);
        assertThat(secondPage.hasMore()).isFalse();
    }

    @Test
    void findStandaloneArticles_ShouldReturnEmptyPageWithoutReadingTopics_WhenPageIsAfterTheLast() {
        StandaloneArticlePage page = standaloneArticles.findStandaloneArticles(5, 2);

        assertThat(page.entries()).isEmpty();
        assertThat(page.hasMore()).isFalse();
        verifyNoInteractions(topicAssignments);
    }

    @Test
    void findStandaloneArticles_ShouldAttachTopicsByNameInOneRead_WhenArticlesHaveTopics() {
        StandaloneArticlePage page = standaloneArticles.findStandaloneArticles(0, 20);

        assertThat(page.entries().get(0).topics()).extracting(TopicLabel::slug)
                .containsExactly("java", "spring");
        assertThat(page.entries().get(1).topics()).isEmpty();
        assertThat(page.entries().get(2).topics()).isEmpty();
        verify(topicAssignments, times(1)).topicLabelsOf(anyCollection());
    }

    @Test
    void findStandaloneArticles_ShouldRejectPaging_WhenPageOrSizeIsOutOfRange() {
        assertThatThrownBy(() -> standaloneArticles.findStandaloneArticles(-1, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> standaloneArticles.findStandaloneArticles(0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<UUID> allIds() {
        return List.of(ALPHA, BETA_FIRST, BETA_SECOND, DRAFT_ID, UNPUBLISHED_ID);
    }

    private static List<UUID> ids(PublicContentPage page) {
        return page.items().stream().map(PublicContentSummary::id).toList();
    }

    private static List<UUID> contentIds(StandaloneArticlePage page) {
        return page.entries().stream().map(entry -> entry.content().id()).toList();
    }
}
