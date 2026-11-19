package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.ContentTestData;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminChildOrderApiIT extends AbstractAdminAuthenticationIT {

    private static final String SERIES_PATH = "/api/admin/v1/series";
    private static final String MODULES_PATH = "/api/admin/v1/modules";

    private static final UUID SERIES = uuid(1);
    private static final UUID MODULE_1 = uuid(2);
    private static final UUID MODULE_2 = uuid(3);
    private static final UUID MODULE_3 = uuid(4);
    private static final UUID ARTICLE_1 = uuid(11);
    private static final UUID ARTICLE_2 = uuid(12);
    private static final UUID ARTICLE_3 = uuid(13);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void insertSeriesWithModulesAndArticles() {
        LearningTestData.deleteAll(jdbcTemplate);
        LearningTestData.insertSeries(jdbcTemplate, SERIES, "a-series");
        LearningTestData.insertModule(jdbcTemplate, MODULE_1, "module-1", SERIES, 1);
        LearningTestData.insertModule(jdbcTemplate, MODULE_2, "module-2", SERIES, 2);
        LearningTestData.insertModule(jdbcTemplate, MODULE_3, "module-3", SERIES, 3);
        insertArticle(ARTICLE_1, "article-1", MODULE_1, 1);
        insertArticle(ARTICLE_2, "article-2", MODULE_1, 2);
        insertArticle(ARTICLE_3, "article-3", MODULE_1, 3);
    }

    @AfterEach
    void removeRows() {
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @Test
    void reorderModules_ShouldChangeOnlyMovedModulesAndSeries_WhenTwoModulesSwap() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                orderPath(SERIES), body("moduleIds", MODULE_2, MODULE_1, MODULE_3), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"2\"");
        assertNoStore(response);
        JsonNode result = JSON.readTree(response.body());
        assertThat(result.propertyNames()).containsExactlyInAnyOrder("seriesId", "revision", "modules");
        assertThat(result.get("seriesId").asString()).isEqualTo(SERIES.toString());
        assertThat(result.get("revision").asLong()).isEqualTo(2);
        assertChild(result.get("modules").get(0), MODULE_2, 1, 2);
        assertChild(result.get("modules").get(1), MODULE_1, 2, 2);
        assertChild(result.get("modules").get(2), MODULE_3, 3, 1);
        assertThat(revisionOf(SERIES)).isEqualTo(2);
        assertThat(revisionOf(MODULE_1)).isEqualTo(2);
        assertThat(revisionOf(MODULE_2)).isEqualTo(2);
        assertThat(revisionOf(MODULE_3)).isEqualTo(1);
        assertModuleSlugs(browser, "module-2", "module-1", "module-3");

        // The ETag of the response is the revision in the database, so the next save works.
        HttpResponse<String> next = browser.put(
                orderPath(SERIES), body("moduleIds", MODULE_1, MODULE_2, MODULE_3), token,
                response.headers().firstValue("ETag").orElseThrow());
        assertThat(next.statusCode()).isEqualTo(200);
        assertThat(next.headers().firstValue("ETag")).contains("\"3\"");
        assertModuleSlugs(browser, "module-1", "module-2", "module-3");
    }

    @Test
    void reorderArticles_ShouldChangeOnlyMovedArticlesAndModule_WhenTwoArticlesSwap() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                articleOrderPath(MODULE_1), body("articleIds", ARTICLE_2, ARTICLE_1, ARTICLE_3),
                token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"2\"");
        JsonNode result = JSON.readTree(response.body());
        assertThat(result.propertyNames()).containsExactlyInAnyOrder("moduleId", "revision", "articles");
        assertThat(result.get("moduleId").asString()).isEqualTo(MODULE_1.toString());
        assertChild(result.get("articles").get(0), ARTICLE_2, 1, 2);
        assertChild(result.get("articles").get(1), ARTICLE_1, 2, 2);
        assertChild(result.get("articles").get(2), ARTICLE_3, 3, 1);
        assertThat(revisionOf(MODULE_1)).isEqualTo(2);
        assertThat(revisionOf(ARTICLE_3)).isEqualTo(1);
        assertArticleSlugs(browser, "article-2", "article-1", "article-3");

        HttpResponse<String> next = browser.put(
                articleOrderPath(MODULE_1), body("articleIds", ARTICLE_1, ARTICLE_2, ARTICLE_3), token,
                response.headers().firstValue("ETag").orElseThrow());
        assertThat(next.statusCode()).isEqualTo(200);
    }

    @Test
    void reorderModules_ShouldRejectRequest_WhenIdsAreNotExactlyTheChildren() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> missing = browser.put(
                orderPath(SERIES), body("moduleIds", MODULE_1, MODULE_2), token, "\"1\"");
        HttpResponse<String> extra = browser.put(
                orderPath(SERIES), body("moduleIds", MODULE_1, MODULE_2, MODULE_3, uuid(99)), token, "\"1\"");
        HttpResponse<String> stranger = browser.put(
                orderPath(SERIES), body("moduleIds", MODULE_1, MODULE_2, uuid(99)), token, "\"1\"");

        assertProblem(missing, 409, "ORDER_SET_CONFLICT");
        assertProblem(extra, 409, "ORDER_SET_CONFLICT");
        assertProblem(stranger, 409, "ORDER_SET_CONFLICT");
        assertModuleSlugs(browser, "module-1", "module-2", "module-3");
        assertThat(revisionOf(SERIES)).isEqualTo(1);
    }

    @Test
    void reorderModules_ShouldRejectRequest_WhenSeriesRevisionIsStale() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        browser.put(orderPath(SERIES), body("moduleIds", MODULE_2, MODULE_1, MODULE_3), token, "\"1\"");

        HttpResponse<String> response = browser.put(
                orderPath(SERIES), body("moduleIds", MODULE_3, MODULE_2, MODULE_1), token, "\"1\"");

        assertProblem(response, 412, "REVISION_CONFLICT");
        assertModuleSlugs(browser, "module-2", "module-1", "module-3");
    }

    @Test
    void reorderModules_ShouldRejectDuplicateIds_BeforeAnyLookup() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        // The Series does not exist, yet the answer is 422, not 404: the check needs no database.
        HttpResponse<String> response = browser.put(
                orderPath(UUID.randomUUID()), body("moduleIds", MODULE_1, MODULE_1), token, "\"1\"");

        assertProblem(response, 422, "VALIDATION_FAILED");
    }

    @Test
    void reorderModules_ShouldChangeNothing_WhenOrderIsUnchanged() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                orderPath(SERIES), body("moduleIds", MODULE_1, MODULE_2, MODULE_3), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
        assertThat(revisionOf(SERIES)).isEqualTo(1);
        assertThat(revisionOf(MODULE_1)).isEqualTo(1);
        assertThat(revisionOf(MODULE_2)).isEqualTo(1);
    }

    @Test
    void reorderArticles_ShouldReturnEmptyList_WhenModuleHasNoArticles() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                articleOrderPath(MODULE_2), body("articleIds"), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
        assertThat(JSON.readTree(response.body()).get("articles").isEmpty()).isTrue();
        assertThat(revisionOf(MODULE_2)).isEqualTo(1);
    }

    @Test
    void reorder_ShouldReturnNotFound_WhenIdIsOfAnotherType() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> moduleAsSeries = browser.put(
                orderPath(MODULE_1), body("moduleIds", MODULE_1), token, "\"1\"");
        HttpResponse<String> seriesAsModule = browser.put(
                articleOrderPath(SERIES), body("articleIds", ARTICLE_1), token, "\"1\"");

        assertProblem(moduleAsSeries, 404, "CONTENT_NOT_FOUND");
        assertProblem(seriesAsModule, 404, "CONTENT_NOT_FOUND");
    }

    @Test
    void reorderModules_ShouldRequirePrecondition_WhenIfMatchIsMissing() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                orderPath(SERIES), body("moduleIds", MODULE_2, MODULE_1, MODULE_3), token, null);

        assertProblem(response, 428, "REVISION_PRECONDITION_REQUIRED");
        assertThat(revisionOf(SERIES)).isEqualTo(1);
    }

    private void insertArticle(UUID id, String slug, UUID moduleId, int position) {
        ContentTestData.insertArticle(jdbcTemplate, id, slug, "Article " + slug, PublicationStatus.DRAFT);
        LearningTestData.insertModuleArticle(jdbcTemplate, id, moduleId, position);
    }

    private static String orderPath(UUID seriesId) {
        return SERIES_PATH + "/" + seriesId + "/module-order";
    }

    private static String articleOrderPath(UUID moduleId) {
        return MODULES_PATH + "/" + moduleId + "/article-order";
    }

    private String body(String key, UUID... ids) throws Exception {
        ObjectNode body = JSON.createObjectNode();
        var array = body.putArray(key);
        for (UUID id : ids) {
            array.add(id.toString());
        }
        return JSON.writeValueAsString(body);
    }

    private static void assertChild(JsonNode child, UUID id, int position, long revision) {
        assertThat(child.propertyNames()).containsExactlyInAnyOrder("id", "position", "revision");
        assertThat(child.get("id").asString()).isEqualTo(id.toString());
        assertThat(child.get("position").asInt()).isEqualTo(position);
        assertThat(child.get("revision").asLong()).isEqualTo(revision);
    }

    private long revisionOf(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT revision FROM content_item WHERE id = ?", Long.class, id);
    }

    /**
     * Reads the Series again. Its Modules must be these slugs, in this order, at positions 1..N.
     */
    private void assertModuleSlugs(Browser browser, String... slugs) throws Exception {
        JsonNode modules = JSON.readTree(browser.get(SERIES_PATH + "/" + SERIES).body()).get("modules");
        assertThat(modules).hasSize(slugs.length);
        for (int index = 0; index < slugs.length; index++) {
            assertThat(modules.get(index).get("slug").asString()).isEqualTo(slugs[index]);
            assertThat(modules.get(index).get("position").asInt()).isEqualTo(index + 1);
        }
    }

    private void assertArticleSlugs(Browser browser, String... slugs) throws Exception {
        JsonNode articles = JSON.readTree(browser.get(MODULES_PATH + "/" + MODULE_1).body()).get("articles");
        assertThat(articles).hasSize(slugs.length);
        for (int index = 0; index < slugs.length; index++) {
            assertThat(articles.get(index).get("slug").asString()).isEqualTo(slugs[index]);
            assertThat(articles.get(index).get("position").asInt()).isEqualTo(index + 1);
        }
    }
}
