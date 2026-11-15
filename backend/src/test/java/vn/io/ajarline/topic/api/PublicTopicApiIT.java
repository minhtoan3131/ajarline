package vn.io.ajarline.topic.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.ContentTestData.insertArticle;
import static vn.io.ajarline.support.TopicTestData.insertAssignment;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
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
class PublicTopicApiIT extends AbstractAdminAuthenticationIT {

    private static final String PUBLIC_TOPICS = "/api/v1/topics";
    private static final UUID JAVA = uuid(1);
    private static final UUID LEGACY = uuid(2);
    private static final UUID HIDDEN = uuid(3);
    private static final UUID ALPHA = uuid(101);
    private static final UUID BETA = uuid(102);
    private static final UUID DRAFT_ID = uuid(103);
    private static final UUID UNPUBLISHED_ID = uuid(104);

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepareTopics() {
        jdbc.update("DELETE FROM content_item");
        jdbc.update("DELETE FROM topic");
        insertTopic(jdbc, JAVA, "java", "Java", true);
        insertTopic(jdbc, LEGACY, "legacy", "Legacy", false);
        insertTopic(jdbc, HIDDEN, "hidden", "Hidden", true);
        insertArticle(jdbc, BETA, "beta-article", "Beta", PublicationStatus.PUBLISHED);
        insertArticle(jdbc, ALPHA, "alpha-article", "Alpha", PublicationStatus.PUBLISHED);
        insertArticle(jdbc, DRAFT_ID, "draft-article", "Draft", PublicationStatus.DRAFT);
        insertArticle(jdbc, UNPUBLISHED_ID, "unpublished-article", "Unpublished",
                PublicationStatus.UNPUBLISHED);
        insertAssignment(jdbc, ALPHA, JAVA);
        insertAssignment(jdbc, ALPHA, LEGACY);
        insertAssignment(jdbc, BETA, JAVA);
        insertAssignment(jdbc, DRAFT_ID, HIDDEN);
        insertAssignment(jdbc, UNPUBLISHED_ID, HIDDEN);
    }

    @Test
    void list_ShouldReturnOnlyTopicsWithPublicContent_WhenCalledAnonymously() throws Exception {
        HttpResponse<String> response = new Browser().get(PUBLIC_TOPICS);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames()).containsExactly("topics");
        assertThat(slugs(body.get("topics"))).containsExactly("java", "legacy");
        assertThat(body.get("topics").get(0).propertyNames())
                .containsExactlyInAnyOrder("id", "slug", "name");
    }

    @Test
    void get_ShouldReturnPublicShapeWithoutActive_WhenInactiveTopicHasPublicContent() throws Exception {
        HttpResponse<String> response = new Browser().get(PUBLIC_TOPICS + "/legacy");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames())
                .containsExactlyInAnyOrder("id", "slug", "name", "description");
        assertThat(body.get("id").asString()).isEqualTo(LEGACY.toString());
        assertThat(body.get("slug").asString()).isEqualTo("legacy");
        assertThat(body.get("name").asString()).isEqualTo("Legacy");
        assertThat(body.get("description").isNull()).isTrue();
    }

    @Test
    void get_ShouldUseSame404_ForMissingTopicAndTopicWithoutPublicContent() throws Exception {
        Browser reader = new Browser();
        HttpResponse<String> missing = reader.get(PUBLIC_TOPICS + "/missing");
        HttpResponse<String> hidden = reader.get(PUBLIC_TOPICS + "/hidden");

        assertTopicNotFound(missing);
        assertTopicNotFound(hidden);
        JsonNode left = JSON.readTree(missing.body());
        JsonNode right = JSON.readTree(hidden.body());
        for (String field : new String[]{"type", "title", "status", "detail", "code"}) {
            assertThat(left.get(field)).isEqualTo(right.get(field));
        }
    }

    @Test
    void content_ShouldReturnExactShape_WhenTopicIsPublic() throws Exception {
        HttpResponse<String> response = new Browser().get(PUBLIC_TOPICS + "/java/content");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames())
                .containsExactlyInAnyOrder("filters", "page", "size", "hasMore", "items");
        assertThat(body.get("filters").get("types").isEmpty()).isTrue();
        assertThat(body.get("page").asInt()).isZero();
        assertThat(body.get("size").asInt()).isEqualTo(20);
        assertThat(body.get("hasMore").asBoolean()).isFalse();
        assertThat(slugs(body.get("items"))).containsExactly("alpha-article", "beta-article");

        JsonNode first = body.get("items").get(0);
        assertThat(first.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "slug", "title", "summary",
                "topics", "publishedAt", "updatedAt", "context");
        assertThat(first.get("id").asString()).isEqualTo(ALPHA.toString());
        assertThat(first.get("type").asString()).isEqualTo("article");
        assertThat(first.get("title").asString()).isEqualTo("Alpha");
        assertThat(first.get("summary").asString()).isEqualTo("Fixture summary");
        assertThat(slugs(first.get("topics"))).containsExactly("java", "legacy");
        assertThat(first.get("context").propertyNames())
                .containsExactlyInAnyOrder("series", "module");
        assertThat(first.get("context").get("series").isNull()).isTrue();
        assertThat(first.get("context").get("module").isNull()).isTrue();
        assertThat(first.get("publishedAt").isNull()).isFalse();
    }

    @Test
    void content_ShouldReportHasMore_WhenAnotherPageExists() throws Exception {
        Browser reader = new Browser();
        JsonNode first = JSON.readTree(reader.get(PUBLIC_TOPICS + "/java/content?size=1").body());
        JsonNode second = JSON.readTree(reader.get(PUBLIC_TOPICS + "/java/content?page=1&size=1").body());
        JsonNode afterLast = JSON.readTree(reader.get(PUBLIC_TOPICS + "/java/content?page=2&size=1").body());

        assertThat(slugs(first.get("items"))).containsExactly("alpha-article");
        assertThat(first.get("hasMore").asBoolean()).isTrue();
        assertThat(slugs(second.get("items"))).containsExactly("beta-article");
        assertThat(second.get("hasMore").asBoolean()).isFalse();
        assertThat(afterLast.get("items").isEmpty()).isTrue();
        assertThat(afterLast.get("hasMore").asBoolean()).isFalse();
        assertThat(afterLast.get("page").asInt()).isEqualTo(2);
    }

    @Test
    void content_ShouldApplyHardTypeFilter_WhenTypesIsGiven() throws Exception {
        Browser reader = new Browser();
        JsonNode articles = JSON.readTree(
                reader.get(PUBLIC_TOPICS + "/java/content?types=article").body());
        HttpResponse<String> seriesResponse = reader.get(PUBLIC_TOPICS + "/java/content?types=series");
        JsonNode series = JSON.readTree(seriesResponse.body());

        assertThat(slugs(articles.get("items"))).containsExactly("alpha-article", "beta-article");
        assertThat(articles.get("filters").get("types").get(0).asString()).isEqualTo("article");
        assertThat(seriesResponse.statusCode()).isEqualTo(200);
        assertThat(series.get("items").isEmpty()).isTrue();
        assertThat(series.get("hasMore").asBoolean()).isFalse();
        assertThat(series.get("filters").get("types").get(0).asString()).isEqualTo("series");
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource({
            "size=0,                   INVALID_PAGE_SIZE",
            "size=51,                  INVALID_PAGE_SIZE",
            "page=-1,                  INVALID_PAGE",
            "types=blog,               INVALID_RESULT_TYPE",
            "'types=article,article',  INVALID_RESULT_TYPE"
    })
    void content_ShouldReject_WhenQueryIsInvalid(String query, String code) throws Exception {
        assertProblem(new Browser().get(PUBLIC_TOPICS + "/java/content?" + query), 400, code);
    }

    @Test
    void content_ShouldReturn404_WhenTopicIsNotPublic() throws Exception {
        Browser reader = new Browser();

        assertTopicNotFound(reader.get(PUBLIC_TOPICS + "/hidden/content"));
        assertTopicNotFound(reader.get(PUBLIC_TOPICS + "/missing/content"));
    }

    private static List<String> slugs(JsonNode array) {
        List<String> slugs = new ArrayList<>();
        for (JsonNode element : array) {
            slugs.add(element.get("slug").asString());
        }
        return slugs;
    }

    private static void assertTopicNotFound(HttpResponse<String> response) {
        assertProblem(response, 404, "CONTENT_NOT_FOUND");
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("title").asString()).isEqualTo("Topic is not publicly available");
        assertThat(body.get("detail").asString()).isEqualTo("Topic is not publicly available.");
    }
}
