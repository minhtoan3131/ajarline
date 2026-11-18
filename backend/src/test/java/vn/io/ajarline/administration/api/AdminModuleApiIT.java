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
class AdminModuleApiIT extends AbstractAdminAuthenticationIT {

    private static final String SERIES = "/api/admin/v1/series";
    private static final String MODULES = "/api/admin/v1/modules";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearContentBeforeTest() {
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @AfterEach
    void clearContentAfterTest() {
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @Test
    void create_ShouldReturnDraftDetail_WhenPayloadIsValid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String seriesId = createSeries(browser, token, "a-series");

        HttpResponse<String> response = createModule(browser, token, seriesId, "module-a", 1);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
        assertNoStore(response);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "seriesId", "position", "title", "slug", "summary",
                "overviewMarkdown", "topicIds", "status", "revision", "publishedAt",
                "unpublishedAt", "createdAt", "updatedAt", "articles");
        assertThat(body.get("type").asString()).isEqualTo("module");
        assertThat(body.get("seriesId").asString()).isEqualTo(seriesId);
        assertThat(body.get("position").asInt()).isEqualTo(1);
        assertThat(body.get("status").asString()).isEqualTo("DRAFT");
        assertThat(body.get("articles").isEmpty()).isTrue();
        assertThat(response.headers().firstValue("Location"))
                .contains(MODULES + "/" + body.get("id").asString());
    }

    @Test
    void create_ShouldKeepPositionsContinuous_WhenModulesAreInsertedAtAnyPlace() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String seriesId = createSeries(browser, token, "a-series");

        createModule(browser, token, seriesId, "module-a", 1);
        createModule(browser, token, seriesId, "module-b", 2);
        createModule(browser, token, seriesId, "module-c", 1);
        createModule(browser, token, seriesId, "module-d", 2);

        assertModules(browser, seriesId, "module-c", "module-d", "module-a", "module-b");
    }

    @Test
    void create_ShouldRejectPosition_WhenOutsideOneToNPlusOne() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String seriesId = createSeries(browser, token, "a-series");

        HttpResponse<String> tooHigh = createModule(browser, token, seriesId, "module-a", 2);
        HttpResponse<String> zero = createModule(browser, token, seriesId, "module-b", 0);

        assertProblem(tooHigh, 422, "INVALID_POSITION");
        assertProblem(zero, 422, "INVALID_POSITION");
        assertThat(rowCount("module")).isZero();
        assertThat(rowCount("content_item")).isEqualTo(1);
    }

    @Test
    void create_ShouldRejectModule_WhenSeriesDoesNotExistOrIsAnotherType() throws Exception {
        ContentTestData.insertArticle(
                jdbcTemplate, uuid(1), "an-article", "An article", PublicationStatus.DRAFT);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> unknown = createModule(
                browser, token, UUID.randomUUID().toString(), "module-a", 1);
        HttpResponse<String> article = createModule(
                browser, token, uuid(1).toString(), "module-b", 1);

        assertProblem(unknown, 422, "SERIES_REFERENCE_NOT_FOUND");
        assertProblem(article, 422, "SERIES_REFERENCE_NOT_FOUND");
        assertThat(rowCount("module")).isZero();
    }

    @Test
    void create_ShouldRejectSecondModule_WhenSlugAlreadyExists() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String seriesId = createSeries(browser, token, "a-series");
        createModule(browser, token, seriesId, "same-slug", 1);

        HttpResponse<String> response = createModule(browser, token, seriesId, "same-slug", 2);

        assertProblem(response, 409, "SLUG_CONFLICT");
        assertModules(browser, seriesId, "same-slug");
    }

    @Test
    void update_ShouldRejectSeriesSave_WhenAModuleWasAddedAfterTheSeriesWasRead() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String seriesId = createSeries(browser, token, "a-series");
        long seriesRevision = revisionOf(browser, SERIES + "/" + seriesId);
        createModule(browser, token, seriesId, "module-a", 1);

        HttpResponse<String> response = browser.put(
                SERIES + "/" + seriesId, seriesPayload("a-series"), token, etag(seriesRevision));

        assertProblem(response, 412, "REVISION_CONFLICT");
        assertThat(revisionOf(browser, SERIES + "/" + seriesId)).isEqualTo(seriesRevision + 1);
    }

    @Test
    void update_ShouldMoveModuleToAnotherSeries_WhenTargetPositionIsValid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String one = createSeries(browser, token, "series-one");
        String two = createSeries(browser, token, "series-two");
        String a = idOf(createModule(browser, token, one, "module-a", 1));
        String b = idOf(createModule(browser, token, one, "module-b", 2));
        String c = idOf(createModule(browser, token, one, "module-c", 3));
        String x = idOf(createModule(browser, token, two, "module-x", 1));
        long oneBefore = revisionOf(browser, SERIES + "/" + one);
        long twoBefore = revisionOf(browser, SERIES + "/" + two);
        long aBefore = revisionOf(browser, MODULES + "/" + a);
        long bBefore = revisionOf(browser, MODULES + "/" + b);
        long cBefore = revisionOf(browser, MODULES + "/" + c);
        long xBefore = revisionOf(browser, MODULES + "/" + x);

        HttpResponse<String> response = browser.put(
                MODULES + "/" + b, modulePayload("module-b", two, 1), token, etag(bBefore));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains(etag(bBefore + 1));
        assertModules(browser, one, "module-a", "module-c");
        assertModules(browser, two, "module-b", "module-x");
        assertThat(revisionOf(browser, SERIES + "/" + one)).isEqualTo(oneBefore + 1);
        assertThat(revisionOf(browser, SERIES + "/" + two)).isEqualTo(twoBefore + 1);
        assertThat(revisionOf(browser, MODULES + "/" + c)).isEqualTo(cBefore + 1);
        assertThat(revisionOf(browser, MODULES + "/" + x)).isEqualTo(xBefore + 1);
        assertThat(revisionOf(browser, MODULES + "/" + a)).isEqualTo(aBefore);

        HttpResponse<String> again = browser.put(
                MODULES + "/" + b, modulePayload("module-b", two, 1), token,
                response.headers().firstValue("ETag").orElseThrow());
        assertThat(again.statusCode()).isEqualTo(200);
    }

    @Test
    void update_ShouldReorderSiblings_WhenModuleMovesInsideItsSeries() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String seriesId = createSeries(browser, token, "a-series");
        String a = idOf(createModule(browser, token, seriesId, "module-a", 1));
        String b = idOf(createModule(browser, token, seriesId, "module-b", 2));
        String c = idOf(createModule(browser, token, seriesId, "module-c", 3));
        long seriesBefore = revisionOf(browser, SERIES + "/" + seriesId);
        long aBefore = revisionOf(browser, MODULES + "/" + a);
        long bBefore = revisionOf(browser, MODULES + "/" + b);
        long cBefore = revisionOf(browser, MODULES + "/" + c);

        HttpResponse<String> response = browser.put(
                MODULES + "/" + a, modulePayload("module-a", seriesId, 3), token, etag(aBefore));

        assertThat(response.statusCode()).isEqualTo(200);
        assertModules(browser, seriesId, "module-b", "module-c", "module-a");
        assertThat(revisionOf(browser, SERIES + "/" + seriesId)).isEqualTo(seriesBefore + 1);
        assertThat(revisionOf(browser, MODULES + "/" + a)).isEqualTo(aBefore + 1);
        assertThat(revisionOf(browser, MODULES + "/" + b)).isEqualTo(bBefore + 1);
        assertThat(revisionOf(browser, MODULES + "/" + c)).isEqualTo(cBefore + 1);
    }

    @Test
    void update_ShouldChangeNothing_WhenFieldsAndPlaceAreUnchanged() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String seriesId = createSeries(browser, token, "a-series");
        String a = idOf(createModule(browser, token, seriesId, "module-a", 1));
        String b = idOf(createModule(browser, token, seriesId, "module-b", 2));
        long seriesBefore = revisionOf(browser, SERIES + "/" + seriesId);
        long aBefore = revisionOf(browser, MODULES + "/" + a);
        long bBefore = revisionOf(browser, MODULES + "/" + b);

        HttpResponse<String> response = browser.put(
                MODULES + "/" + b, modulePayload("module-b", seriesId, 2), token, etag(bBefore));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains(etag(bBefore));
        assertThat(revisionOf(browser, SERIES + "/" + seriesId)).isEqualTo(seriesBefore);
        assertThat(revisionOf(browser, MODULES + "/" + a)).isEqualTo(aBefore);
    }

    @Test
    void update_ShouldRollBackBothSeries_WhenTopicDoesNotExist() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String one = createSeries(browser, token, "series-one");
        String two = createSeries(browser, token, "series-two");
        createModule(browser, token, one, "module-a", 1);
        String b = idOf(createModule(browser, token, one, "module-b", 2));
        createModule(browser, token, two, "module-x", 1);
        long oneBefore = revisionOf(browser, SERIES + "/" + one);
        long twoBefore = revisionOf(browser, SERIES + "/" + two);
        long bBefore = revisionOf(browser, MODULES + "/" + b);
        ObjectNode body = modulePayloadNode("module-b", two, 1);
        body.putArray("topicIds").add(UUID.randomUUID().toString());

        HttpResponse<String> response = browser.put(
                MODULES + "/" + b, JSON.writeValueAsString(body), token, etag(bBefore));

        assertProblem(response, 422, "TOPIC_REFERENCE_NOT_FOUND");
        assertModules(browser, one, "module-a", "module-b");
        assertModules(browser, two, "module-x");
        assertThat(revisionOf(browser, SERIES + "/" + one)).isEqualTo(oneBefore);
        assertThat(revisionOf(browser, SERIES + "/" + two)).isEqualTo(twoBefore);
        assertThat(revisionOf(browser, MODULES + "/" + b)).isEqualTo(bBefore);
    }

    @Test
    void update_ShouldRejectPublishedModule_WhenOverviewIsRemoved() throws Exception {
        UUID seriesId = uuid(1);
        UUID moduleId = uuid(2);
        LearningTestData.insertSeries(jdbcTemplate, seriesId, "a-series");
        LearningTestData.insertModule(
                jdbcTemplate, moduleId, "published-module", seriesId, 1,
                PublicationStatus.PUBLISHED, "# Overview");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        ObjectNode body = modulePayloadNode("published-module", seriesId.toString(), 1);
        body.putNull("overviewMarkdown");

        HttpResponse<String> response = browser.put(
                MODULES + "/" + moduleId, JSON.writeValueAsString(body), token, "\"1\"");

        assertProblem(response, 422, "MODULE_OVERVIEW_REQUIRED");
        assertThat(revisionOf(browser, MODULES + "/" + moduleId)).isEqualTo(1);
    }

    @Test
    void get_ShouldListArticlesInPositionOrder_WhenModuleHasArticles() throws Exception {
        UUID seriesId = uuid(1);
        UUID moduleId = uuid(2);
        LearningTestData.insertSeries(jdbcTemplate, seriesId, "a-series");
        LearningTestData.insertModule(jdbcTemplate, moduleId, "a-module", seriesId, 1);
        ContentTestData.insertArticle(
                jdbcTemplate, uuid(3), "second-article", "Second", PublicationStatus.DRAFT);
        ContentTestData.insertArticle(
                jdbcTemplate, uuid(4), "first-article", "First", PublicationStatus.PUBLISHED);
        LearningTestData.insertModuleArticle(jdbcTemplate, uuid(3), moduleId, 2);
        LearningTestData.insertModuleArticle(jdbcTemplate, uuid(4), moduleId, 1);
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.get(MODULES + "/" + moduleId);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode articles = JSON.readTree(response.body()).get("articles");
        assertThat(articles).hasSize(2);
        assertThat(articles.get(0).get("slug").asString()).isEqualTo("first-article");
        assertThat(articles.get(0).get("status").asString()).isEqualTo("PUBLISHED");
        assertThat(articles.get(0).get("position").asInt()).isEqualTo(1);
        assertThat(articles.get(1).get("slug").asString()).isEqualTo("second-article");
    }

    @Test
    void get_ShouldReturnNotFound_WhenIdBelongsToASeries() throws Exception {
        LearningTestData.insertSeries(jdbcTemplate, uuid(1), "a-series");
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.get(MODULES + "/" + uuid(1));

        assertProblem(response, 404, "CONTENT_NOT_FOUND");
    }

    private String createSeries(Browser browser, String token, String slug) throws Exception {
        HttpResponse<String> created = browser.post(SERIES, seriesPayload(slug), token);
        assertThat(created.statusCode()).isEqualTo(201);
        return idOf(created);
    }

    private HttpResponse<String> createModule(
            Browser browser, String token, String seriesId, String slug, int position) throws Exception {
        return browser.post(MODULES, modulePayload(slug, seriesId, position), token);
    }

    private String seriesPayload(String slug) throws Exception {
        ObjectNode body = JSON.createObjectNode();
        body.put("title", "Series " + slug);
        body.put("slug", slug);
        body.put("summary", "Series summary.");
        body.put("overviewMarkdown", "# Series overview");
        body.putArray("topicIds");
        return JSON.writeValueAsString(body);
    }

    private ObjectNode modulePayloadNode(String slug, String seriesId, int position) {
        ObjectNode body = JSON.createObjectNode();
        body.put("seriesId", seriesId);
        body.put("position", position);
        body.put("title", "Module " + slug);
        body.put("slug", slug);
        body.put("summary", "Module summary.");
        body.put("overviewMarkdown", "# Module overview");
        body.putArray("topicIds");
        return body;
    }

    private String modulePayload(String slug, String seriesId, int position) throws Exception {
        return JSON.writeValueAsString(modulePayloadNode(slug, seriesId, position));
    }

    private static String idOf(HttpResponse<String> response) {
        return JSON.readTree(response.body()).get("id").asString();
    }

    private static String etag(long revision) {
        return "\"" + revision + "\"";
    }

    private long revisionOf(Browser browser, String path) throws Exception {
        HttpResponse<String> response = browser.get(path);
        assertThat(response.statusCode()).isEqualTo(200);
        return Long.parseLong(response.headers().firstValue("ETag").orElseThrow().replace("\"", ""));
    }

    /**
     * Reads the Series again. Its Modules must be these slugs, in this order, at positions 1..N.
     */
    private void assertModules(Browser browser, String seriesId, String... slugs) throws Exception {
        JsonNode modules = JSON.readTree(browser.get(SERIES + "/" + seriesId).body()).get("modules");
        assertThat(modules).hasSize(slugs.length);
        for (int index = 0; index < slugs.length; index++) {
            assertThat(modules.get(index).get("slug").asString()).isEqualTo(slugs[index]);
            assertThat(modules.get(index).get("position").asInt()).isEqualTo(index + 1);
        }
    }

    private long rowCount(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
