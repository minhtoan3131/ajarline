package vn.io.ajarline.content.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.ContentTestData.insertArticle;
import static vn.io.ajarline.support.NewsTestData.insertCompleteDraft;
import static vn.io.ajarline.support.NewsTestData.insertNews;
import static vn.io.ajarline.support.TopicTestData.insertAssignment;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.administration.api.AbstractAdminAuthenticationIT;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class
})
class PublicNewsCollectionApiIT extends AbstractAdminAuthenticationIT {

    private static final String NEWS = "/api/v1/news";
    private static final String ADMIN_CONTENT = "/api/admin/v1/content";
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-11-12T00:00:00Z");
    private static final UUID SPRING = uuid(11);
    private static final UUID JAVA = uuid(12);

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        // news and content_topic rows go with their content_item; topic is restricted, so it goes last.
        jdbc.update("DELETE FROM content_item");
        jdbc.update("DELETE FROM topic");
        insertTopic(jdbc, SPRING, "spring", "Spring", true);
        insertTopic(jdbc, JAVA, "java", "Java", true);
    }

    @Test
    void list_ShouldReturnExactShape_WhenNewsIsPublished() throws Exception {
        insertPublished(uuid(1), "only-news", BASE_TIME);

        HttpResponse<String> response = new Browser().get(NEWS);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames())
                .containsExactlyInAnyOrder("page", "size", "hasMore", "items");
        assertThat(body.get("page").asInt()).isZero();
        assertThat(body.get("size").asInt()).isEqualTo(20);
        assertThat(body.get("hasMore").asBoolean()).isFalse();

        JsonNode item = body.get("items").get(0);
        assertThat(item.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "slug", "title", "summary", "topics", "publishedAt", "updatedAt");
        assertThat(item.get("id").asString()).isEqualTo(uuid(1).toString());
        assertThat(item.get("type").asString()).isEqualTo("news");
        assertThat(item.get("title").asString()).isEqualTo("News only-news");
        assertThat(item.get("summary").asString()).isEqualTo("Summary of only-news");
        assertThat(item.get("publishedAt").isNull()).isFalse();
        for (String hidden : new String[]{"source", "bodyMarkdown", "status", "revision"}) {
            assertThat(item.has(hidden)).as(hidden).isFalse();
        }
    }

    @Test
    void list_ShouldOrderNewestPublishedFirstThenById_WhenTimesAreEqual() throws Exception {
        OffsetDateTime sameTime = BASE_TIME.minusDays(1);
        // Inserted out of order on purpose.
        insertPublished(uuid(4), "tie-b", sameTime);
        insertPublished(uuid(1), "oldest", BASE_TIME.minusDays(2));
        insertPublished(uuid(3), "tie-a", sameTime);
        insertPublished(uuid(2), "newest", BASE_TIME);

        JsonNode body = JSON.readTree(new Browser().get(NEWS).body());

        assertThat(slugs(body.get("items")))
                .containsExactly("newest", "tie-a", "tie-b", "oldest");
    }

    @Test
    void list_ShouldHideNews_WhenItIsDraftOrUnpublished() throws Exception {
        insertPublished(uuid(1), "public-news", BASE_TIME);
        insertNews(jdbc, uuid(2), "draft-news", PublicationStatus.DRAFT,
                "A summary", "# Body", "Source", "https://example.com/a");
        insertNews(jdbc, uuid(3), "unpublished-news", PublicationStatus.UNPUBLISHED,
                "A summary", "# Body", "Source", "https://example.com/a");

        JsonNode body = JSON.readTree(new Browser().get(NEWS).body());

        assertThat(slugs(body.get("items"))).containsExactly("public-news");
    }

    @Test
    void list_ShouldCutPagesAfterOrdering_WhenMoreThanOnePageExists() throws Exception {
        for (int i = 0; i <= 20; i++) {
            insertPublished(uuid(100 + i), "news-%02d".formatted(i), BASE_TIME.plusMinutes(i));
        }
        Browser reader = new Browser();

        JsonNode first = JSON.readTree(reader.get(NEWS).body());
        JsonNode second = JSON.readTree(reader.get(NEWS + "?page=1").body());
        HttpResponse<String> afterLast = reader.get(NEWS + "?page=2");

        assertThat(first.get("items").size()).isEqualTo(20);
        assertThat(first.get("hasMore").asBoolean()).isTrue();
        assertThat(slugs(first.get("items")).getFirst()).isEqualTo("news-20");
        assertThat(slugs(first.get("items")).getLast()).isEqualTo("news-01");
        assertThat(slugs(second.get("items"))).containsExactly("news-00");
        assertThat(second.get("hasMore").asBoolean()).isFalse();
        assertThat(afterLast.statusCode()).isEqualTo(200);
        JsonNode empty = JSON.readTree(afterLast.body());
        assertThat(empty.get("items").isEmpty()).isTrue();
        assertThat(empty.get("hasMore").asBoolean()).isFalse();
        assertThat(empty.get("page").asInt()).isEqualTo(2);
    }

    @Test
    void list_ShouldReturnEmptyPageWithoutError_WhenNoNewsIsPublic() throws Exception {
        HttpResponse<String> response = new Browser().get(NEWS);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("items").isEmpty()).isTrue();
        assertThat(body.get("hasMore").asBoolean()).isFalse();
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource({
            "page=-1,   INVALID_PAGE",
            "page=abc,  INVALID_PAGE",
            "size=0,    INVALID_PAGE_SIZE",
            "size=51,   INVALID_PAGE_SIZE",
            "size=abc,  INVALID_PAGE_SIZE"
    })
    void list_ShouldReject_WhenQueryIsInvalid(String query, String code) throws Exception {
        assertProblem(new Browser().get(NEWS + "?" + query), 400, code);
    }

    @Test
    void list_ShouldGiveEachNewsItsOwnTopics_WhenTopicsAreReadForThePage() throws Exception {
        insertPublished(uuid(1), "both-topics", BASE_TIME.plusMinutes(3));
        insertPublished(uuid(2), "spring-only", BASE_TIME.plusMinutes(2));
        insertPublished(uuid(3), "no-topic", BASE_TIME.plusMinutes(1));
        insertAssignment(jdbc, uuid(1), SPRING);
        insertAssignment(jdbc, uuid(1), JAVA);
        insertAssignment(jdbc, uuid(2), SPRING);

        JsonNode items = JSON.readTree(new Browser().get(NEWS).body()).get("items");

        assertThat(slugs(items)).containsExactly("both-topics", "spring-only", "no-topic");
        // Topics are ordered by name: Java before Spring.
        assertThat(slugs(items.get(0).get("topics"))).containsExactly("java", "spring");
        assertThat(items.get(0).get("topics").get(0).propertyNames())
                .containsExactlyInAnyOrder("id", "slug", "name");
        assertThat(slugs(items.get(1).get("topics"))).containsExactly("spring");
        assertThat(items.get(2).get("topics").isEmpty()).isTrue();
    }

    @Test
    void list_ShouldStillListNews_WhenItsNewsRowIsMissing() throws Exception {
        insertPublished(uuid(1), "broken-news", BASE_TIME);
        jdbc.update("DELETE FROM news WHERE content_id = ?", uuid(1));

        HttpResponse<String> response = new Browser().get(NEWS);

        // The list does not read the news table; the broken data shows when the item is opened.
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(slugs(JSON.readTree(response.body()).get("items"))).containsExactly("broken-news");
        assertProblem(new Browser().get(NEWS + "/broken-news"), 500, "INTERNAL_ERROR");
    }

    @Test
    void list_ShouldLeaveNewsOutOfStandaloneArticles_WhenBothArePublished() throws Exception {
        insertArticle(jdbc, uuid(1), "an-article", "An Article", PublicationStatus.PUBLISHED);
        insertPublished(uuid(2), "a-news", BASE_TIME);

        JsonNode articles = JSON.readTree(
                new Browser().get("/api/v1/articles?standalone=true").body());

        assertThat(slugs(articles.get("items"))).containsExactly("an-article");
    }

    @Test
    void lifecycle_ShouldChangeListImmediately() throws Exception {
        Browser admin = authenticatedBrowser();
        String token = csrf(admin);
        insertCompleteDraft(jdbc, uuid(1), "lifecycle-news");
        Browser reader = new Browser();

        assertThat(slugs(JSON.readTree(reader.get(NEWS).body()).get("items"))).isEmpty();

        assertThat(admin.post(ADMIN_CONTENT + "/" + uuid(1) + "/publish",
                null, token, "\"1\"").statusCode()).isEqualTo(200);
        assertThat(slugs(JSON.readTree(reader.get(NEWS).body()).get("items")))
                .containsExactly("lifecycle-news");

        assertThat(admin.post(ADMIN_CONTENT + "/" + uuid(1) + "/unpublish",
                null, token, "\"2\"").statusCode()).isEqualTo(200);
        assertThat(slugs(JSON.readTree(reader.get(NEWS).body()).get("items"))).isEmpty();
    }

    private void insertPublished(UUID id, String slug, OffsetDateTime publishedAt) {
        insertNews(jdbc, id, slug, PublicationStatus.PUBLISHED,
                "Summary of " + slug, "# Body", "Source", "https://example.com/" + slug);
        jdbc.update("UPDATE content_item SET published_at = ? WHERE id = ?", publishedAt, id);
    }

    private static List<String> slugs(JsonNode array) {
        List<String> slugs = new ArrayList<>();
        for (JsonNode element : array) {
            slugs.add(element.get("slug").asString());
        }
        return slugs;
    }
}
