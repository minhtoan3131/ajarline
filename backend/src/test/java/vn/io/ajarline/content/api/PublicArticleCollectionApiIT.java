package vn.io.ajarline.content.api;

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
class PublicArticleCollectionApiIT extends AbstractAdminAuthenticationIT {

    private static final String ARTICLES = "/api/v1/articles";
    private static final String STANDALONE = ARTICLES + "?standalone=true";
    private static final UUID SPRING = uuid(1);
    private static final UUID JAVA = uuid(2);
    private static final UUID BETA = uuid(101);
    private static final UUID ALPHA = uuid(102);
    private static final UUID DRAFT_ID = uuid(103);
    private static final UUID UNPUBLISHED_ID = uuid(104);

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepareArticles() {
        jdbc.update("DELETE FROM content_item");
        jdbc.update("DELETE FROM topic");
        insertTopic(jdbc, SPRING, "spring", "Spring", true);
        insertTopic(jdbc, JAVA, "java", "Java", true);
        insertArticle(jdbc, BETA, "beta-article", "Beta", PublicationStatus.PUBLISHED);
        insertArticle(jdbc, ALPHA, "alpha-article", "Alpha", PublicationStatus.PUBLISHED);
        insertArticle(jdbc, DRAFT_ID, "draft-article", "Draft", PublicationStatus.DRAFT);
        insertArticle(jdbc, UNPUBLISHED_ID, "unpublished-article", "Unpublished",
                PublicationStatus.UNPUBLISHED);
        insertAssignment(jdbc, ALPHA, SPRING);
        insertAssignment(jdbc, ALPHA, JAVA);
    }

    @Test
    void list_ShouldReturnExactShapeOfPublishedArticles_WhenCalledAnonymously() throws Exception {
        HttpResponse<String> response = new Browser().get(STANDALONE);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames())
                .containsExactlyInAnyOrder("page", "size", "hasMore", "items");
        assertThat(body.get("page").asInt()).isZero();
        assertThat(body.get("size").asInt()).isEqualTo(20);
        assertThat(body.get("hasMore").asBoolean()).isFalse();
        assertThat(slugs(body.get("items"))).containsExactly("alpha-article", "beta-article");

        JsonNode first = body.get("items").get(0);
        assertThat(first.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "slug", "title", "summary", "topics", "publishedAt", "updatedAt");
        assertThat(first.get("id").asString()).isEqualTo(ALPHA.toString());
        assertThat(first.get("type").asString()).isEqualTo("article");
        assertThat(first.get("title").asString()).isEqualTo("Alpha");
        assertThat(first.get("summary").asString()).isEqualTo("Fixture summary");
        assertThat(first.get("publishedAt").isNull()).isFalse();
        assertThat(first.get("updatedAt").isNull()).isFalse();
        assertThat(slugs(first.get("topics"))).containsExactly("java", "spring");
        assertThat(first.get("topics").get(0).propertyNames())
                .containsExactlyInAnyOrder("id", "slug", "name");
        assertThat(body.get("items").get(1).get("topics").isEmpty()).isTrue();
    }

    @Test
    void list_ShouldReportHasMore_WhenAnotherPageExists() throws Exception {
        Browser reader = new Browser();
        JsonNode first = JSON.readTree(reader.get(STANDALONE + "&size=1").body());
        JsonNode second = JSON.readTree(reader.get(STANDALONE + "&page=1&size=1").body());
        JsonNode afterLast = JSON.readTree(reader.get(STANDALONE + "&page=2&size=1").body());

        assertThat(slugs(first.get("items"))).containsExactly("alpha-article");
        assertThat(first.get("hasMore").asBoolean()).isTrue();
        assertThat(slugs(second.get("items"))).containsExactly("beta-article");
        assertThat(second.get("hasMore").asBoolean()).isFalse();
        assertThat(afterLast.get("items").isEmpty()).isTrue();
        assertThat(afterLast.get("hasMore").asBoolean()).isFalse();
        assertThat(afterLast.get("page").asInt()).isEqualTo(2);
        assertThat(afterLast.get("size").asInt()).isEqualTo(1);
    }

    @Test
    void list_ShouldReturnEmptyPageWithoutError_WhenNoArticleIsPublic() throws Exception {
        jdbc.update("DELETE FROM content_item");

        HttpResponse<String> response = new Browser().get(STANDALONE);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("items").isEmpty()).isTrue();
        assertThat(body.get("hasMore").asBoolean()).isFalse();
    }

    @Test
    void list_ShouldIgnoreUnknownParameters_WhenStandaloneIsTrue() throws Exception {
        HttpResponse<String> response = new Browser().get(STANDALONE + "&types=series&foo=bar");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(slugs(JSON.readTree(response.body()).get("items")))
                .containsExactly("alpha-article", "beta-article");
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource({
            "'',                                 INVALID_ARTICLE_COLLECTION_MODE",
            "page=0,                             INVALID_ARTICLE_COLLECTION_MODE",
            "standalone=false,                   INVALID_ARTICLE_COLLECTION_MODE",
            "standalone=TRUE,                    INVALID_ARTICLE_COLLECTION_MODE",
            "standalone=1,                       INVALID_ARTICLE_COLLECTION_MODE",
            "'standalone=true&standalone=true',  INVALID_ARTICLE_COLLECTION_MODE",
            "page=-1,                            INVALID_ARTICLE_COLLECTION_MODE",
            "'standalone=true&page=-1',          INVALID_PAGE",
            "'standalone=true&page=abc',         INVALID_PAGE",
            "'standalone=true&size=0',           INVALID_PAGE_SIZE",
            "'standalone=true&size=51',          INVALID_PAGE_SIZE"
    })
    void list_ShouldReject_WhenQueryIsInvalid(String query, String code) throws Exception {
        assertProblem(new Browser().get(ARTICLES + "?" + query), 400, code);
    }

    private static List<String> slugs(JsonNode array) {
        List<String> slugs = new ArrayList<>();
        for (JsonNode element : array) {
            slugs.add(element.get("slug").asString());
        }
        return slugs;
    }
}
