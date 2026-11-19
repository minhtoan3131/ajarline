package vn.io.ajarline.learning.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.LearningTestData.MODULE_A1;
import static vn.io.ajarline.support.LearningTestData.SERIES_A;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
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
class PublicSeriesApiIT extends AbstractAdminAuthenticationIT {

    private static final String SERIES = "/api/v1/series/";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

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
    void get_ShouldReturnExactContract_WhenSeriesIsPublic() throws Exception {
        HttpResponse<String> response = new Browser().get(SERIES + "series-a");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "slug", "title", "summary", "overviewMarkdown",
                "topics", "modules", "publishedAt", "updatedAt");
        assertThat(body.get("type").asString()).isEqualTo("series");
        assertThat(body.get("overviewMarkdown").asString()).isEqualTo("# Series A");
        assertThat(body.has("status")).isFalse();
        assertThat(body.has("revision")).isFalse();
        JsonNode firstModule = body.get("modules").get(0);
        assertThat(firstModule.propertyNames()).containsExactlyInAnyOrder(
                "id", "slug", "title", "summary", "position", "articles");
        assertThat(firstModule.get("articles").get(0).propertyNames()).containsExactlyInAnyOrder(
                "id", "slug", "title", "summary", "position");
    }

    @Test
    void get_ShouldRankModulesAndArticlesAmongPublicOnes_WhenMiddleOnesAreHidden() throws Exception {
        JsonNode body = JSON.readTree(new Browser().get(SERIES + "series-a").body());

        JsonNode modules = body.get("modules");
        assertThat(modules).hasSize(2);
        assertThat(modules.get(0).get("slug").asString()).isEqualTo("module-a1");
        assertThat(modules.get(0).get("position").asInt()).isEqualTo(1);
        // Module A3 is a draft between A1 and A2, so A2 has rank 2, not the stored position 3.
        assertThat(modules.get(1).get("slug").asString()).isEqualTo("module-a2");
        assertThat(modules.get(1).get("position").asInt()).isEqualTo(2);
        assertThat(slugsOf(modules.get(0).get("articles"))).containsExactly("article-a1", "article-a3");
        // Article A2 is a draft between A1 and A3, so A3 has rank 2, not the stored position 3.
        assertThat(modules.get(0).get("articles").get(1).get("position").asInt()).isEqualTo(2);
        assertThat(slugsOf(modules.get(1).get("articles"))).containsExactly("article-a4");
    }

    @Test
    void get_ShouldNotShowAnyHiddenSlugOrTitle_WhenChildrenAreHidden() throws Exception {
        String body = new Browser().get(SERIES + "series-a").body();

        assertThat(body).doesNotContain("module-a3", "article-a2", "article-a5");
    }

    @Test
    void get_ShouldGiveTheSameNotFound_WhenSeriesIsNotPublicForAnyReason() throws Exception {
        LearningTestData.insertSeries(
                jdbc, uuid(3001), "draft-series", PublicationStatus.DRAFT, "# Overview");
        LearningTestData.insertSeries(
                jdbc, uuid(3002), "unpublished-series", PublicationStatus.UNPUBLISHED, "# Overview");

        JsonNode expected = problemOf("no-such-series");

        // Draft, unpublished, published with no public Module, and the slug of a Module.
        for (String slug : List.of("draft-series", "unpublished-series", "series-b", "module-a1")) {
            assertThat(problemOf(slug)).isEqualTo(expected);
        }
    }

    @Test
    void get_ShouldReturnInternalError_WhenPublicSeriesHasNoOverview() throws Exception {
        jdbc.update("UPDATE series SET overview_markdown = '' WHERE content_id = ?", SERIES_A);

        HttpResponse<String> response = new Browser().get(SERIES + "series-a");

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(JSON.readTree(response.body()).get("code").asString()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void get_ShouldNotWaitOrChangeRevision_WhenAnotherTransactionLocksTheRows() throws Exception {
        long revisionBefore = revisionOf(SERIES_A);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        TransactionStatus adminTransaction = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            jdbc.queryForList(
                    "SELECT id FROM content_item WHERE id IN (?, ?) FOR UPDATE",
                    UUID.class, SERIES_A, MODULE_A1);

            Future<HttpResponse<String>> reading = executor.submit(
                    () -> new Browser().get(SERIES + "series-a"));

            assertThat(reading.get(10, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
        } finally {
            transactionManager.rollback(adminTransaction);
            executor.shutdownNow();
        }
        assertThat(revisionOf(SERIES_A)).isEqualTo(revisionBefore);
    }

    private JsonNode problemOf(String slug) throws Exception {
        HttpResponse<String> response = new Browser().get(SERIES + slug);
        assertThat(response.statusCode()).isEqualTo(404);
        ObjectNode problem = (ObjectNode) JSON.readTree(response.body());
        problem.remove("requestId");
        assertThat(problem.get("code").asString()).isEqualTo("CONTENT_NOT_FOUND");
        return problem;
    }

    private long revisionOf(UUID id) {
        return jdbc.queryForObject("SELECT revision FROM content_item WHERE id = ?", Long.class, id);
    }

    private static List<String> slugsOf(JsonNode items) {
        List<String> slugs = new ArrayList<>();
        items.forEach(item -> slugs.add(item.get("slug").asString()));
        return slugs;
    }
}
