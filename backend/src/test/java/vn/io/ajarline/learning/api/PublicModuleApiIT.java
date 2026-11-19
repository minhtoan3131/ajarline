package vn.io.ajarline.learning.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.LearningTestData.MODULE_A1;
import static vn.io.ajarline.support.LearningTestData.SERIES_A;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import vn.io.ajarline.administration.api.AbstractAdminAuthenticationIT;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class
})
class PublicModuleApiIT extends AbstractAdminAuthenticationIT {

    private static final String MODULES = "/api/v1/modules/";
    private static final String ARTICLES = "/api/v1/articles/";

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void insertScenarios() {
        removeRows();
        LearningTestData.insertStandardScenarios(jdbc);
    }

    @AfterEach
    void removeRows() {
        LearningTestData.deleteAll(jdbc);
    }

    @Test
    void get_ShouldReturnExactContract_WhenModuleIsPublic() throws Exception {
        HttpResponse<String> response = new Browser().get(MODULES + "module-a1");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "slug", "title", "summary", "overviewMarkdown",
                "topics", "series", "articles", "publishedAt", "updatedAt");
        assertThat(body.get("type").asString()).isEqualTo("module");
        assertThat(body.get("overviewMarkdown").asString()).isEqualTo("# Module A1");
        assertThat(body.get("series").propertyNames()).containsExactlyInAnyOrder("id", "slug", "title");
        assertThat(body.get("series").get("slug").asString()).isEqualTo("series-a");
        JsonNode articles = body.get("articles");
        assertThat(articles).hasSize(2);
        assertThat(articles.get(0).get("slug").asString()).isEqualTo("article-a1");
        assertThat(articles.get(1).get("slug").asString()).isEqualTo("article-a3");
        // Article A2 is a draft between them, so the rank of A3 is 2, not the stored position 3.
        assertThat(articles.get(1).get("position").asInt()).isEqualTo(2);
    }

    @Test
    void get_ShouldHaveNoSeries_WhenOnlyTheSeriesIsHidden() throws Exception {
        HttpResponse<String> response = new Browser().get(MODULES + "module-c1");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(response.body()).get("series").isNull()).isTrue();
        assertThat(response.body()).doesNotContain("series-c");
    }

    @Test
    void get_ShouldNotShowAnyHiddenSlugOrTitle_WhenArticlesAreHidden() throws Exception {
        String body = new Browser().get(MODULES + "module-a1").body();

        assertThat(body).doesNotContain("article-a2", "module-a3");
    }

    @Test
    void get_ShouldGiveTheSameNotFound_WhenModuleIsNotPublicForAnyReason() throws Exception {
        LearningTestData.insertModule(
                jdbc, uuid(3003), "unpublished-module", SERIES_A, 4,
                PublicationStatus.UNPUBLISHED, "# Overview");

        JsonNode expected = problemOf("no-such-module");

        // A draft, an unpublished one, published with no public Article, and the slug of a Series.
        for (String slug : List.of("module-a3", "unpublished-module", "module-d1", "series-a")) {
            assertThat(problemOf(slug)).isEqualTo(expected);
        }
    }

    @Test
    void get_ShouldReturnInternalError_WhenPublicModuleHasNoOverview() throws Exception {
        jdbc.update("UPDATE module SET overview_markdown = ' ' WHERE content_id = ?", MODULE_A1);

        HttpResponse<String> response = new Browser().get(MODULES + "module-a1");

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(JSON.readTree(response.body()).get("code").asString()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void position_ShouldEqualPreviousPositionPlusOne_WhenSameArticleIsReadFromModuleAndFromArticle()
            throws Exception {
        for (String moduleSlug : List.of("module-a1", "module-a2", "module-c1")) {
            JsonNode module = JSON.readTree(new Browser().get(MODULES + moduleSlug).body());
            for (JsonNode listed : module.get("articles")) {
                JsonNode article = JSON.readTree(
                        new Browser().get(ARTICLES + listed.get("slug").asString()).body());

                JsonNode previous = article.get("previousArticle");
                int expected = previous.isNull() ? 1 : previous.get("position").asInt() + 1;
                assertThat(listed.get("position").asInt()).isEqualTo(expected);
            }
        }
    }

    private JsonNode problemOf(String slug) throws Exception {
        HttpResponse<String> response = new Browser().get(MODULES + slug);
        assertThat(response.statusCode()).isEqualTo(404);
        ObjectNode problem = (ObjectNode) JSON.readTree(response.body());
        problem.remove("requestId");
        assertThat(problem.get("code").asString()).isEqualTo("CONTENT_NOT_FOUND");
        return problem;
    }
}
