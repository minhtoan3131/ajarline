package vn.io.ajarline.content.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicContentPage;
import vn.io.ajarline.content.contract.PublicContentQuery;
import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.ContentTestData.insertArticle;
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
    private static final Set<ContentType> ALL_TYPES = EnumSet.allOf(ContentType.class);

    @Autowired
    private PublicContentQuery publicContent;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("DELETE FROM content_item");
        // Inserted out of order so the test proves the query sorts.
        insertArticle(jdbcTemplate, BETA_SECOND, "beta-second", "Beta", PublicationStatus.PUBLISHED);
        insertArticle(jdbcTemplate, ALPHA, "alpha", "Alpha", PublicationStatus.PUBLISHED);
        insertArticle(jdbcTemplate, BETA_FIRST, "beta-first", "Beta", PublicationStatus.PUBLISHED);
        insertArticle(jdbcTemplate, DRAFT_ID, "draft", "Draft", PublicationStatus.DRAFT);
        insertArticle(jdbcTemplate, UNPUBLISHED_ID, "unpublished", "Unpublished", PublicationStatus.UNPUBLISHED);
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

    private static List<UUID> allIds() {
        return List.of(ALPHA, BETA_FIRST, BETA_SECOND, DRAFT_ID, UNPUBLISHED_ID);
    }

    private static List<UUID> ids(PublicContentPage page) {
        return page.items().stream().map(PublicContentSummary::id).toList();
    }
}
