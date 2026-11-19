package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.ContentTestData;
import vn.io.ajarline.support.NewsTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminNewsApiIT extends AbstractAdminAuthenticationIT {

    private static final String NEWS = "/api/admin/v1/news";
    private static final String ARTICLES = "/api/admin/v1/articles";
    private static final String CONTENT = "/api/admin/v1/content";
    private static final String SOURCE_NAME = "A source";
    private static final String SOURCE_URL = "https://example.com/story";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearContentBeforeTest() {
        jdbcTemplate.update("DELETE FROM content_item");
    }

    @AfterEach
    void clearContentAfterTest() {
        jdbcTemplate.update("DELETE FROM content_item");
    }

    @Test
    void create_ShouldReturnDraftDetail_WhenPayloadIsValid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(NEWS, payload("a-news-item"), token);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
        assertNoStore(response);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "title", "slug", "summary", "bodyMarkdown", "source", "topicIds",
                "status", "revision", "publishedAt", "unpublishedAt", "createdAt", "updatedAt");
        assertThat(body.get("type").asString()).isEqualTo("news");
        assertThat(body.get("status").asString()).isEqualTo("DRAFT");
        assertThat(body.get("revision").asLong()).isEqualTo(1);
        assertThat(body.get("source").propertyNames())
                .containsExactlyInAnyOrder("name", "url", "publishedAt");
        assertThat(body.get("source").get("name").asString()).isEqualTo(SOURCE_NAME);
        assertThat(body.get("source").get("publishedAt").isNull()).isTrue();
        assertThat(response.headers().firstValue("Location"))
                .contains(NEWS + "/" + body.get("id").asString());
        assertThat(rowCount("content_item")).isEqualTo(1);
        assertThat(rowCount("news")).isEqualTo(1);
    }

    @Test
    void create_ShouldKeepEmptySource_WhenDraftHasNoSource() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        ObjectNode body = payloadNode("no-source");
        body.set("source", sourceNode(null, null, null));

        HttpResponse<String> response = browser.post(NEWS, JSON.writeValueAsString(body), token);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode source = JSON.readTree(response.body()).get("source");
        assertThat(source.get("name").isNull()).isTrue();
        assertThat(source.get("url").isNull()).isTrue();
        assertThat(source.get("publishedAt").isNull()).isTrue();
    }

    @ParameterizedTest(name = "[{index}] rejects {0}")
    @ValueSource(strings = {
            "javascript:alert(1)", "data:text/html,x", "ftp://example.com",
            "//example.com", "example.com", "https://exa mple.com"})
    void create_ShouldRejectUnsafeSourceUrl_WhenUrlIsNotHttpOrHttps(String url) throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        ObjectNode body = payloadNode("unsafe-url");
        body.set("source", sourceNode(SOURCE_NAME, url, null));

        HttpResponse<String> response = browser.post(NEWS, JSON.writeValueAsString(body), token);

        assertProblem(response, 422, "VALIDATION_FAILED");
        assertThat(rowCount("content_item")).isZero();
        assertThat(rowCount("news")).isZero();
    }

    @ParameterizedTest(name = "[{index}] rejects {0}")
    @ValueSource(strings = {"2026-11-18", "2026-11-18T10:00"})
    void create_ShouldReturnBadRequest_WhenPublishedAtHasNoOffset(String publishedAt) throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        ObjectNode body = payloadNode("no-offset");
        body.set("source", sourceNode(SOURCE_NAME, SOURCE_URL, publishedAt));

        HttpResponse<String> response = browser.post(NEWS, JSON.writeValueAsString(body), token);

        assertProblem(response, 400, "INVALID_REQUEST");
        assertThat(rowCount("content_item")).isZero();
    }

    @Test
    void create_ShouldRejectSecondNews_WhenSlugAlreadyExists() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        browser.post(NEWS, payload("same-slug"), token);

        HttpResponse<String> response = browser.post(NEWS, payload("same-slug"), token);

        assertProblem(response, 409, "SLUG_CONFLICT");
        assertThat(rowCount("content_item")).isEqualTo(1);
        assertThat(rowCount("news")).isEqualTo(1);
    }

    @Test
    void create_ShouldRollBackBothParts_WhenTopicDoesNotExist() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        ObjectNode body = payloadNode("unknown-topic");
        body.putArray("topicIds").add(UUID.randomUUID().toString());

        HttpResponse<String> response = browser.post(NEWS, JSON.writeValueAsString(body), token);

        assertProblem(response, 422, "TOPIC_REFERENCE_NOT_FOUND");
        assertThat(rowCount("content_item")).isZero();
        assertThat(rowCount("news")).isZero();
    }

    @Test
    void get_ShouldReturnNotFound_WhenIdBelongsToAnArticle() throws Exception {
        ContentTestData.insertArticle(
                jdbcTemplate, uuid(1), "an-article", "An article", PublicationStatus.DRAFT);
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.get(NEWS + "/" + uuid(1));

        assertProblem(response, 404, "CONTENT_NOT_FOUND");
    }

    @Test
    void get_ShouldReturnNotFound_WhenArticleEndpointIsGivenANewsId() throws Exception {
        NewsTestData.insertCompleteDraft(jdbcTemplate, uuid(1), "a-news-item");
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.get(ARTICLES + "/" + uuid(1));

        assertProblem(response, 404, "CONTENT_NOT_FOUND");
    }

    @Test
    void get_ShouldReturnServerError_WhenNewsRowIsMissing() throws Exception {
        NewsTestData.insertCompleteDraft(jdbcTemplate, uuid(1), "broken-news");
        jdbcTemplate.update("DELETE FROM news WHERE content_id = ?", uuid(1));
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.get(NEWS + "/" + uuid(1));

        assertProblem(response, 500, "INTERNAL_ERROR");
    }

    @Test
    void update_ShouldKeepRevision_WhenPayloadIsUnchanged() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createNews(browser, token, "same-state");

        HttpResponse<String> response = browser.put(NEWS + "/" + id, payload("same-state"), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
    }

    @Test
    void update_ShouldRaiseRevisionOnce_WhenOnlyBodyChanges() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createNews(browser, token, "body-only");
        ObjectNode body = payloadNode("body-only");
        body.put("bodyMarkdown", "# A new body");

        HttpResponse<String> response = browser.put(
                NEWS + "/" + id, JSON.writeValueAsString(body), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"2\"");
        assertThat(JSON.readTree(response.body()).get("bodyMarkdown").asString())
                .isEqualTo("# A new body");
    }

    @Test
    void update_ShouldRaiseRevisionOnce_WhenOnlyTheSourceTimeChanges() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createNews(browser, token, "time-only");
        ObjectNode body = payloadNode("time-only");
        body.set("source", sourceNode(SOURCE_NAME, SOURCE_URL, "2026-11-18T03:00:00Z"));

        HttpResponse<String> response = browser.put(
                NEWS + "/" + id, JSON.writeValueAsString(body), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"2\"");
    }

    @Test
    void update_ShouldKeepRevision_WhenSameMomentIsSentWithAnotherOffset() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        ObjectNode first = payloadNode("same-moment");
        first.set("source", sourceNode(SOURCE_NAME, SOURCE_URL, "2026-11-18T10:00:00+07:00"));
        HttpResponse<String> created = browser.post(NEWS, JSON.writeValueAsString(first), token);
        JsonNode source = JSON.readTree(created.body()).get("source");
        // The server answers in UTC. It is the same moment, so it is not a change.
        assertThat(Instant.parse(source.get("publishedAt").asString()))
                .isEqualTo(Instant.parse("2026-11-18T03:00:00Z"));
        String id = JSON.readTree(created.body()).get("id").asString();
        ObjectNode again = payloadNode("same-moment");
        again.set("source", sourceNode(SOURCE_NAME, SOURCE_URL, "2026-11-18T03:00:00Z"));

        HttpResponse<String> response = browser.put(
                NEWS + "/" + id, JSON.writeValueAsString(again), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
    }

    @Test
    void update_ShouldAcceptReturnedETag_WhenSavedThenPublished() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createNews(browser, token, "saved-then-published");
        ObjectNode body = payloadNode("saved-then-published");
        body.put("summary", "A new summary.");
        HttpResponse<String> saved = browser.put(
                NEWS + "/" + id, JSON.writeValueAsString(body), token, "\"1\"");
        String etag = saved.headers().firstValue("ETag").orElseThrow();

        HttpResponse<String> published = browser.post(
                CONTENT + "/" + id + "/publish", null, token, etag);

        assertThat(published.statusCode()).isEqualTo(200);
        assertThat(published.headers().firstValue("ETag")).contains("\"3\"");
    }

    @Test
    void update_ShouldRejectStaleRevision_WhenTheSameETagIsUsedTwice() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createNews(browser, token, "stale-revision");
        ObjectNode body = payloadNode("stale-revision");
        body.put("summary", "A new summary.");
        browser.put(NEWS + "/" + id, JSON.writeValueAsString(body), token, "\"1\"");
        body.put("summary", "Another summary.");

        HttpResponse<String> response = browser.put(
                NEWS + "/" + id, JSON.writeValueAsString(body), token, "\"1\"");

        assertProblem(response, 412, "REVISION_CONFLICT");
    }

    @Test
    void update_ShouldRollBackBody_WhenTopicDoesNotExist() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createNews(browser, token, "rollback-body");
        ObjectNode body = payloadNode("rollback-body");
        body.put("bodyMarkdown", "# A changed body");
        body.putArray("topicIds").add(UUID.randomUUID().toString());

        HttpResponse<String> response = browser.put(
                NEWS + "/" + id, JSON.writeValueAsString(body), token, "\"1\"");

        assertProblem(response, 422, "TOPIC_REFERENCE_NOT_FOUND");
        HttpResponse<String> reread = browser.get(NEWS + "/" + id);
        assertThat(reread.headers().firstValue("ETag")).contains("\"1\"");
        assertThat(JSON.readTree(reread.body()).get("bodyMarkdown").asString()).isEqualTo("# Body");
    }

    @Test
    void update_ShouldRejectPublishedNews_WhenSourceUrlIsRemoved() throws Exception {
        UUID id = insertPublishedNews("published-news");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        ObjectNode body = payloadNode("published-news");
        body.set("source", sourceNode(SOURCE_NAME, null, null));

        HttpResponse<String> response = browser.put(
                NEWS + "/" + id, JSON.writeValueAsString(body), token, "\"1\"");

        assertProblem(response, 422, "NEWS_SOURCE_REQUIRED");
        assertThat(browser.get(NEWS + "/" + id).headers().firstValue("ETag")).contains("\"1\"");
    }

    @Test
    void update_ShouldRejectPublishedNews_WhenBodyIsRemoved() throws Exception {
        UUID id = insertPublishedNews("published-news");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        ObjectNode body = payloadNode("published-news");
        body.putNull("bodyMarkdown");

        HttpResponse<String> response = browser.put(
                NEWS + "/" + id, JSON.writeValueAsString(body), token, "\"1\"");

        assertProblem(response, 422, "NEWS_BODY_REQUIRED");
        assertThat(browser.get(NEWS + "/" + id).headers().firstValue("ETag")).contains("\"1\"");
    }

    @Test
    void update_ShouldAcceptPublishedNews_WhenSummaryBodyAndSourceStayPresent() throws Exception {
        UUID id = insertPublishedNews("published-news");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        ObjectNode body = payloadNode("published-news");
        body.put("summary", "A newer summary.");

        HttpResponse<String> response = browser.put(
                NEWS + "/" + id, JSON.writeValueAsString(body), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"2\"");
    }

    @Test
    void update_ShouldAcceptDraftNews_WhenSourceIsRemoved() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createNews(browser, token, "draft-news");
        ObjectNode body = payloadNode("draft-news");
        body.set("source", sourceNode(null, null, null));

        HttpResponse<String> response = browser.put(
                NEWS + "/" + id, JSON.writeValueAsString(body), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void update_ShouldRejectSlugChange_WhenNewsWasPublishedBefore() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createNews(browser, token, "first-slug");
        browser.post(CONTENT + "/" + id + "/publish", null, token, "\"1\"");
        browser.post(CONTENT + "/" + id + "/unpublish", null, token, "\"2\"");

        HttpResponse<String> response = browser.put(
                NEWS + "/" + id, payload("another-slug"), token, "\"3\"");

        assertProblem(response, 409, "SLUG_IMMUTABLE_AFTER_PUBLICATION");
    }

    private UUID insertPublishedNews(String slug) {
        UUID id = uuid(1);
        NewsTestData.insertNews(jdbcTemplate, id, slug, PublicationStatus.PUBLISHED,
                "A short summary.", "# Body", SOURCE_NAME, SOURCE_URL);
        return id;
    }

    private String createNews(Browser browser, String token, String slug) throws Exception {
        HttpResponse<String> created = browser.post(NEWS, payload(slug), token);
        assertThat(created.statusCode()).isEqualTo(201);
        return JSON.readTree(created.body()).get("id").asString();
    }

    private ObjectNode payloadNode(String slug) {
        ObjectNode body = JSON.createObjectNode();
        body.put("title", "A News Item");
        body.put("slug", slug);
        body.put("summary", "A short summary.");
        body.put("bodyMarkdown", "# Body");
        body.set("source", sourceNode(SOURCE_NAME, SOURCE_URL, null));
        body.putArray("topicIds");
        return body;
    }

    private ObjectNode sourceNode(String name, String url, String publishedAt) {
        ObjectNode source = JSON.createObjectNode();
        putTextOrNull(source, "name", name);
        putTextOrNull(source, "url", url);
        putTextOrNull(source, "publishedAt", publishedAt);
        return source;
    }

    private static void putTextOrNull(ObjectNode node, String key, String value) {
        if (value == null) {
            node.putNull(key);
        } else {
            node.put(key, value);
        }
    }

    private String payload(String slug) throws Exception {
        return JSON.writeValueAsString(payloadNode(slug));
    }

    private long rowCount(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
