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
class AdminSeriesApiIT extends AbstractAdminAuthenticationIT {

    private static final String SERIES = "/api/admin/v1/series";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearContentBeforeTest() {
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @AfterEach
    void clearContentAfterTest() {
        // Plain DELETE FROM content_item in other test classes is not safe when
        // hierarchy rows are left behind.
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @Test
    void create_ShouldReturnDraftDetail_WhenPayloadIsValid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(
                SERIES, payload("java-concurrency", "A short summary.", "# Overview"), token);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
        assertNoStore(response);
        JsonNode body = JSON.readTree(response.body());
        assertDetail(body, "java-concurrency", "# Overview", 1);
        assertThat(response.headers().firstValue("Location"))
                .contains(SERIES + "/" + body.get("id").asString());
        assertThat(body.get("modules").isEmpty()).isTrue();
        assertThat(rowCount("content_item")).isEqualTo(1);
        assertThat(rowCount("series")).isEqualTo(1);
    }

    @Test
    void create_ShouldRejectSecondSeries_WhenSlugAlreadyExists() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        browser.post(SERIES, payload("same-slug", "A short summary.", "# Overview"), token);

        HttpResponse<String> response = browser.post(
                SERIES, payload("same-slug", "Another summary.", "# Other"), token);

        assertProblem(response, 409, "SLUG_CONFLICT");
        assertThat(rowCount("content_item")).isEqualTo(1);
        assertThat(rowCount("series")).isEqualTo(1);
    }

    @Test
    void create_ShouldRollBackBothParts_WhenTopicDoesNotExist() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        ObjectNode body = payloadNode("unknown-topic", "A short summary.", "# Overview");
        body.putArray("topicIds").add(UUID.randomUUID().toString());

        HttpResponse<String> response = browser.post(
                SERIES, JSON.writeValueAsString(body), token);

        assertProblem(response, 422, "TOPIC_REFERENCE_NOT_FOUND");
        assertThat(rowCount("content_item")).isZero();
        assertThat(rowCount("series")).isZero();
    }

    @Test
    void get_ShouldListModulesInPositionOrder_WhenSeriesHasModules() throws Exception {
        UUID seriesId = uuid(1);
        LearningTestData.insertSeries(jdbcTemplate, seriesId, "a-series");
        LearningTestData.insertModule(
                jdbcTemplate, uuid(2), "second-module", seriesId, 2,
                PublicationStatus.DRAFT, "Overview");
        LearningTestData.insertModule(
                jdbcTemplate, uuid(3), "first-module", seriesId, 1,
                PublicationStatus.PUBLISHED, "Overview");
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.get(SERIES + "/" + seriesId);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode modules = JSON.readTree(response.body()).get("modules");
        assertThat(modules).hasSize(2);
        assertThat(modules.get(0).propertyNames()).containsExactlyInAnyOrder(
                "id", "title", "slug", "status", "position");
        assertThat(modules.get(0).get("id").asString()).isEqualTo(uuid(3).toString());
        assertThat(modules.get(0).get("slug").asString()).isEqualTo("first-module");
        assertThat(modules.get(0).get("status").asString()).isEqualTo("PUBLISHED");
        assertThat(modules.get(0).get("position").asInt()).isEqualTo(1);
        assertThat(modules.get(1).get("slug").asString()).isEqualTo("second-module");
        assertThat(modules.get(1).get("position").asInt()).isEqualTo(2);
    }

    @Test
    void get_ShouldReturnNotFound_WhenIdBelongsToAnArticle() throws Exception {
        ContentTestData.insertArticle(
                jdbcTemplate, uuid(1), "an-article", "An article", PublicationStatus.DRAFT);
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.get(SERIES + "/" + uuid(1));

        assertProblem(response, 404, "CONTENT_NOT_FOUND");
    }

    @Test
    void update_ShouldKeepRevision_WhenPayloadIsUnchanged() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createSeries(browser, token, "same-state");

        HttpResponse<String> response = browser.put(
                SERIES + "/" + id, payload("same-state", "A short summary.", "# Overview"),
                token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
    }

    @Test
    void update_ShouldRaiseRevisionOnce_WhenOnlyOverviewChanges() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createSeries(browser, token, "overview-only");

        HttpResponse<String> response = browser.put(
                SERIES + "/" + id, payload("overview-only", "A short summary.", "# New overview"),
                token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"2\"");
        assertThat(JSON.readTree(response.body()).get("overviewMarkdown").asString())
                .isEqualTo("# New overview");
    }

    @Test
    void update_ShouldAcceptReturnedETag_WhenSavedTwice() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createSeries(browser, token, "saved-twice");
        HttpResponse<String> first = browser.put(
                SERIES + "/" + id, payload("saved-twice", "First summary.", "# Overview"),
                token, "\"1\"");
        String etag = first.headers().firstValue("ETag").orElseThrow();

        HttpResponse<String> second = browser.put(
                SERIES + "/" + id, payload("saved-twice", "Second summary.", "# Overview"),
                token, etag);

        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(second.headers().firstValue("ETag")).contains("\"3\"");
    }

    @Test
    void update_ShouldRejectStaleRevision_WhenIfMatchIsOld() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createSeries(browser, token, "stale-revision");
        browser.put(SERIES + "/" + id,
                payload("stale-revision", "New summary.", "# Overview"), token, "\"1\"");

        HttpResponse<String> response = browser.put(
                SERIES + "/" + id,
                payload("stale-revision", "Older summary.", "# Overview"), token, "\"1\"");

        assertProblem(response, 412, "REVISION_CONFLICT");
    }

    @Test
    void update_ShouldRejectPublishedSeries_WhenSummaryIsRemoved() throws Exception {
        UUID id = insertPublishedSeries("published-series");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                SERIES + "/" + id, payload("published-series", null, "# Overview"),
                token, "\"1\"");

        assertProblem(response, 422, "CONTENT_SUMMARY_REQUIRED");
        assertThat(browser.get(SERIES + "/" + id).headers().firstValue("ETag")).contains("\"1\"");
    }

    @Test
    void update_ShouldRejectPublishedSeries_WhenOverviewIsRemoved() throws Exception {
        UUID id = insertPublishedSeries("published-series");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                SERIES + "/" + id, payload("published-series", "A short summary.", null),
                token, "\"1\"");

        assertProblem(response, 422, "SERIES_OVERVIEW_REQUIRED");
        assertThat(browser.get(SERIES + "/" + id).headers().firstValue("ETag")).contains("\"1\"");
    }

    @Test
    void update_ShouldAcceptPublishedSeries_WhenSummaryAndOverviewStayPresent() throws Exception {
        UUID id = insertPublishedSeries("published-series");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                SERIES + "/" + id,
                payload("published-series", "A new summary.", "# New overview"), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"2\"");
    }

    @Test
    void update_ShouldRejectSlugChange_WhenSeriesWasPublished() throws Exception {
        UUID id = insertPublishedSeries("published-series");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                SERIES + "/" + id,
                payload("another-slug", "A short summary.", "# Overview"), token, "\"1\"");

        assertProblem(response, 409, "SLUG_IMMUTABLE_AFTER_PUBLICATION");
    }

    private UUID insertPublishedSeries(String slug) {
        UUID id = uuid(1);
        LearningTestData.insertSeries(
                jdbcTemplate, id, slug, PublicationStatus.PUBLISHED, "# Overview");
        return id;
    }

    private String createSeries(Browser browser, String token, String slug) throws Exception {
        HttpResponse<String> created = browser.post(
                SERIES, payload(slug, "A short summary.", "# Overview"), token);
        assertThat(created.statusCode()).isEqualTo(201);
        return JSON.readTree(created.body()).get("id").asString();
    }

    private ObjectNode payloadNode(String slug, String summary, String overviewMarkdown) {
        ObjectNode body = JSON.createObjectNode();
        body.put("title", "Java Concurrency");
        body.put("slug", slug);
        if (summary == null) {
            body.putNull("summary");
        } else {
            body.put("summary", summary);
        }
        if (overviewMarkdown == null) {
            body.putNull("overviewMarkdown");
        } else {
            body.put("overviewMarkdown", overviewMarkdown);
        }
        body.putArray("topicIds");
        return body;
    }

    private String payload(String slug, String summary, String overviewMarkdown) throws Exception {
        return JSON.writeValueAsString(payloadNode(slug, summary, overviewMarkdown));
    }

    private static void assertDetail(JsonNode body, String slug, String overview, long revision) {
        assertThat(body.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "title", "slug", "summary", "overviewMarkdown", "topicIds",
                "status", "revision", "publishedAt", "unpublishedAt", "createdAt", "updatedAt",
                "modules");
        assertThat(body.get("type").asString()).isEqualTo("series");
        assertThat(body.get("slug").asString()).isEqualTo(slug);
        assertThat(body.get("overviewMarkdown").asString()).isEqualTo(overview);
        assertThat(body.get("status").asString()).isEqualTo("DRAFT");
        assertThat(body.get("revision").asLong()).isEqualTo(revision);
        assertThat(body.get("topicIds").isEmpty()).isTrue();
        assertThat(body.get("publishedAt").isNull()).isTrue();
        assertThat(body.get("unpublishedAt").isNull()).isTrue();
    }

    private long rowCount(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
