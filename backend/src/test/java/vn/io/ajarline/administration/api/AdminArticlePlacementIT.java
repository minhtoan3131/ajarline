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
class AdminArticlePlacementIT extends AbstractAdminAuthenticationIT {

    private static final String ARTICLES = "/api/admin/v1/articles";
    private static final String MODULES = "/api/admin/v1/modules";

    private static final UUID SERIES = uuid(1);
    private static final UUID MODULE_ONE = uuid(2);
    private static final UUID MODULE_TWO = uuid(3);
    private static final UUID ARTICLE_A = uuid(11);
    private static final UUID ARTICLE_B = uuid(12);
    private static final UUID ARTICLE_C = uuid(13);
    private static final UUID ARTICLE_D = uuid(14);
    private static final UUID ARTICLE_X = uuid(15);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void insertSeriesAndTwoModules() {
        LearningTestData.deleteAll(jdbcTemplate);
        LearningTestData.insertSeries(jdbcTemplate, SERIES, "a-series");
        LearningTestData.insertModule(jdbcTemplate, MODULE_ONE, "module-one", SERIES, 1);
        LearningTestData.insertModule(jdbcTemplate, MODULE_TWO, "module-two", SERIES, 2);
    }

    @AfterEach
    void removeRows() {
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @Test
    void update_ShouldInsertArticleAtPosition_WhenArticleWasStandalone() throws Exception {
        insertArticle(ARTICLE_A, "article-a", MODULE_ONE, 1);
        insertArticle(ARTICLE_B, "article-b", MODULE_ONE, 2);
        insertArticle(ARTICLE_X, "article-x", null, 0);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + ARTICLE_X, payload("article-x", MODULE_ONE, 2), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"2\"");
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("moduleId").asString()).isEqualTo(MODULE_ONE.toString());
        assertThat(body.get("position").asInt()).isEqualTo(2);
        assertModuleArticles(browser, MODULE_ONE, "article-a", "article-x", "article-b");
        assertThat(revisionOf(ARTICLE_X)).isEqualTo(2);
        assertThat(revisionOf(MODULE_ONE)).isEqualTo(2);
        assertThat(revisionOf(ARTICLE_B)).isEqualTo(2);
        assertThat(revisionOf(ARTICLE_A)).isEqualTo(1);

        HttpResponse<String> again = browser.put(
                ARTICLES + "/" + ARTICLE_X, payload("article-x", MODULE_ONE, 2), token,
                response.headers().firstValue("ETag").orElseThrow());
        assertThat(again.statusCode()).isEqualTo(200);
        assertThat(again.headers().firstValue("ETag")).contains("\"2\"");
    }

    @Test
    void update_ShouldMoveArticleToAnotherModule_WhenTargetPositionIsValid() throws Exception {
        insertArticle(ARTICLE_A, "article-a", MODULE_ONE, 1);
        insertArticle(ARTICLE_X, "article-x", MODULE_ONE, 2);
        insertArticle(ARTICLE_C, "article-c", MODULE_ONE, 3);
        insertArticle(ARTICLE_D, "article-d", MODULE_TWO, 1);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + ARTICLE_X, payload("article-x", MODULE_TWO, 1), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertModuleArticles(browser, MODULE_ONE, "article-a", "article-c");
        assertModuleArticles(browser, MODULE_TWO, "article-x", "article-d");
        assertThat(revisionOf(ARTICLE_X)).isEqualTo(2);
        assertThat(revisionOf(MODULE_ONE)).isEqualTo(2);
        assertThat(revisionOf(MODULE_TWO)).isEqualTo(2);
        assertThat(revisionOf(ARTICLE_C)).isEqualTo(2);
        assertThat(revisionOf(ARTICLE_D)).isEqualTo(2);
        assertThat(revisionOf(ARTICLE_A)).isEqualTo(1);
    }

    @Test
    void update_ShouldMakeArticleStandalone_WhenPlacementIsNull() throws Exception {
        insertArticle(ARTICLE_A, "article-a", MODULE_ONE, 1);
        insertArticle(ARTICLE_X, "article-x", MODULE_ONE, 2);
        insertArticle(ARTICLE_C, "article-c", MODULE_ONE, 3);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + ARTICLE_X, payload("article-x", null, null), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("moduleId").isNull()).isTrue();
        assertThat(body.get("position").isNull()).isTrue();
        assertModuleArticles(browser, MODULE_ONE, "article-a", "article-c");
        assertThat(rowCount("module_article")).isEqualTo(2);
        assertThat(revisionOf(ARTICLE_X)).isEqualTo(2);
        assertThat(revisionOf(MODULE_ONE)).isEqualTo(2);
        assertThat(revisionOf(ARTICLE_C)).isEqualTo(2);
        assertThat(revisionOf(ARTICLE_A)).isEqualTo(1);
    }

    @Test
    void update_ShouldChangeNothing_WhenPlacementIsUnchanged() throws Exception {
        insertArticle(ARTICLE_A, "article-a", MODULE_ONE, 1);
        insertArticle(ARTICLE_X, "article-x", MODULE_ONE, 2);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + ARTICLE_X, payload("article-x", MODULE_ONE, 2), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
        assertThat(revisionOf(ARTICLE_X)).isEqualTo(1);
        assertThat(revisionOf(ARTICLE_A)).isEqualTo(1);
        assertThat(revisionOf(MODULE_ONE)).isEqualTo(1);
    }

    @Test
    void update_ShouldRejectPlacement_WhenModuleDoesNotExistOrIsAnotherType() throws Exception {
        insertArticle(ARTICLE_X, "article-x", null, 0);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> unknown = browser.put(
                ARTICLES + "/" + ARTICLE_X, payload("article-x", UUID.randomUUID(), 1), token, "\"1\"");
        HttpResponse<String> series = browser.put(
                ARTICLES + "/" + ARTICLE_X, payload("article-x", SERIES, 1), token, "\"1\"");

        assertProblem(unknown, 422, "MODULE_REFERENCE_NOT_FOUND");
        assertProblem(series, 422, "MODULE_REFERENCE_NOT_FOUND");
        assertThat(rowCount("module_article")).isZero();
        assertThat(revisionOf(ARTICLE_X)).isEqualTo(1);
    }

    @Test
    void update_ShouldRejectPlacement_WhenPositionIsOutsideOneToNPlusOne() throws Exception {
        insertArticle(ARTICLE_A, "article-a", MODULE_ONE, 1);
        insertArticle(ARTICLE_X, "article-x", null, 0);
        insertArticle(ARTICLE_B, "article-b", MODULE_TWO, 1);
        insertArticle(ARTICLE_C, "article-c", MODULE_TWO, 2);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        // The Module has one Article, so the valid positions for a new one are 1 and 2.
        HttpResponse<String> tooHigh = browser.put(
                ARTICLES + "/" + ARTICLE_X, payload("article-x", MODULE_ONE, 3), token, "\"1\"");
        // The Article is already in this Module. N does not count it, so 3 is too high.
        HttpResponse<String> tooHighInside = browser.put(
                ARTICLES + "/" + ARTICLE_C, payload("article-c", MODULE_TWO, 3), token, "\"1\"");

        assertProblem(tooHigh, 422, "INVALID_POSITION");
        assertProblem(tooHighInside, 422, "INVALID_POSITION");
        assertModuleArticles(browser, MODULE_ONE, "article-a");
        assertModuleArticles(browser, MODULE_TWO, "article-b", "article-c");
        assertThat(revisionOf(ARTICLE_X)).isEqualTo(1);
        assertThat(revisionOf(MODULE_ONE)).isEqualTo(1);
    }

    @Test
    void update_ShouldRollBackBothModules_WhenTopicDoesNotExist() throws Exception {
        insertArticle(ARTICLE_A, "article-a", MODULE_ONE, 1);
        insertArticle(ARTICLE_X, "article-x", MODULE_ONE, 2);
        insertArticle(ARTICLE_C, "article-c", MODULE_ONE, 3);
        insertArticle(ARTICLE_D, "article-d", MODULE_TWO, 1);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        ObjectNode body = payloadNode("article-x", MODULE_TWO, 1);
        body.putArray("topicIds").add(UUID.randomUUID().toString());

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + ARTICLE_X, JSON.writeValueAsString(body), token, "\"1\"");

        assertProblem(response, 422, "TOPIC_REFERENCE_NOT_FOUND");
        assertModuleArticles(browser, MODULE_ONE, "article-a", "article-x", "article-c");
        assertModuleArticles(browser, MODULE_TWO, "article-d");
        assertThat(revisionOf(MODULE_ONE)).isEqualTo(1);
        assertThat(revisionOf(MODULE_TWO)).isEqualTo(1);
        assertThat(revisionOf(ARTICLE_X)).isEqualTo(1);
        assertThat(revisionOf(ARTICLE_C)).isEqualTo(1);
    }

    @Test
    void update_ShouldRejectModuleSave_WhenAnArticleWasPlacedAfterTheModuleWasRead() throws Exception {
        insertArticle(ARTICLE_X, "article-x", null, 0);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        HttpResponse<String> read = browser.get(MODULES + "/" + MODULE_ONE);
        String moduleEtag = read.headers().firstValue("ETag").orElseThrow();
        browser.put(ARTICLES + "/" + ARTICLE_X, payload("article-x", MODULE_ONE, 1), token, "\"1\"");

        HttpResponse<String> response = browser.put(
                MODULES + "/" + MODULE_ONE, modulePayload("module-one"), token, moduleEtag);

        assertProblem(response, 412, "REVISION_CONFLICT");
    }

    @Test
    void update_ShouldReturnNotFound_WhenArticleDoesNotExistEvenIfTheModuleIsWrong() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + UUID.randomUUID(),
                payload("missing", UUID.randomUUID(), 1), token, "\"1\"");

        assertProblem(response, 404, "CONTENT_NOT_FOUND");
    }

    @Test
    void create_ShouldPlaceNewArticle_WhenPlacementIsGiven() throws Exception {
        insertArticle(ARTICLE_A, "article-a", MODULE_ONE, 1);
        insertArticle(ARTICLE_B, "article-b", MODULE_ONE, 2);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(
                ARTICLES, payload("new-article", MODULE_ONE, 1), token);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("moduleId").asString()).isEqualTo(MODULE_ONE.toString());
        assertThat(body.get("position").asInt()).isEqualTo(1);
        assertModuleArticles(browser, MODULE_ONE, "new-article", "article-a", "article-b");
        assertThat(revisionOf(MODULE_ONE)).isEqualTo(2);
        assertThat(revisionOf(ARTICLE_A)).isEqualTo(2);
        assertThat(revisionOf(ARTICLE_B)).isEqualTo(2);
    }

    @Test
    void get_ShouldReturnPlacement_WhenArticleIsInAModule() throws Exception {
        insertArticle(ARTICLE_A, "article-a", MODULE_ONE, 1);
        insertArticle(ARTICLE_X, "article-x", null, 0);
        Browser browser = authenticatedBrowser();

        JsonNode member = JSON.readTree(browser.get(ARTICLES + "/" + ARTICLE_A).body());
        JsonNode standalone = JSON.readTree(browser.get(ARTICLES + "/" + ARTICLE_X).body());

        assertThat(member.get("moduleId").asString()).isEqualTo(MODULE_ONE.toString());
        assertThat(member.get("position").asInt()).isEqualTo(1);
        assertThat(standalone.get("moduleId").isNull()).isTrue();
        assertThat(standalone.get("position").isNull()).isTrue();
    }

    /**
     * Inserts a Draft Article whose fields match {@link #payloadNode}. A module id of null
     * leaves it standalone.
     */
    private void insertArticle(UUID id, String slug, UUID moduleId, int position) {
        ContentTestData.insertArticle(jdbcTemplate, id, slug, "Article " + slug, PublicationStatus.DRAFT);
        if (moduleId != null) {
            LearningTestData.insertModuleArticle(jdbcTemplate, id, moduleId, position);
        }
    }

    private ObjectNode payloadNode(String slug, UUID moduleId, Integer position) {
        ObjectNode body = JSON.createObjectNode();
        body.put("title", "Article " + slug);
        body.put("slug", slug);
        body.put("summary", "Fixture summary");
        body.put("bodyMarkdown", "# Fixture body");
        if (moduleId == null) {
            body.putNull("moduleId");
            body.putNull("position");
        } else {
            body.put("moduleId", moduleId.toString());
            body.put("position", position);
        }
        body.putArray("topicIds");
        return body;
    }

    private String payload(String slug, UUID moduleId, Integer position) throws Exception {
        return JSON.writeValueAsString(payloadNode(slug, moduleId, position));
    }

    private String modulePayload(String slug) throws Exception {
        ObjectNode body = JSON.createObjectNode();
        body.put("seriesId", SERIES.toString());
        body.put("position", 1);
        body.put("title", "Fixture " + slug);
        body.put("slug", slug);
        body.put("summary", "Fixture summary");
        body.put("overviewMarkdown", "Fixture overview");
        body.putArray("topicIds");
        return JSON.writeValueAsString(body);
    }

    private long revisionOf(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT revision FROM content_item WHERE id = ?", Long.class, id);
    }

    /**
     * Reads the Module again. Its Articles must be these slugs, in this order, at positions 1..N.
     */
    private void assertModuleArticles(Browser browser, UUID moduleId, String... slugs) throws Exception {
        JsonNode articles = JSON.readTree(browser.get(MODULES + "/" + moduleId).body()).get("articles");
        assertThat(articles).hasSize(slugs.length);
        for (int index = 0; index < slugs.length; index++) {
            assertThat(articles.get(index).get("slug").asString()).isEqualTo(slugs[index]);
            assertThat(articles.get(index).get("position").asInt()).isEqualTo(index + 1);
        }
    }

    private long rowCount(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
