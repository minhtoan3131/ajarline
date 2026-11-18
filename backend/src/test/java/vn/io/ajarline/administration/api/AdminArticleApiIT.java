package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminArticleApiIT extends AbstractAdminAuthenticationIT {

    private static final String ARTICLES = "/api/admin/v1/articles";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearArticles() {
        jdbcTemplate.update("DELETE FROM content_item");
    }

    @Test
    void create_ShouldReturnDraftDetail_WhenStandalonePayloadIsValid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(
                ARTICLES, editable("first-article", "# First body"), token);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
        assertNoStore(response);

        JsonNode body = JSON.readTree(response.body());
        assertDetail(body, "first-article", "# First body", 1);
        assertThat(response.headers().firstValue("Location"))
                .contains(ARTICLES + "/" + body.get("id").asString());
        assertThat(rowCount("content_item")).isEqualTo(1);
        assertThat(rowCount("article")).isEqualTo(1);
    }

    @Test
    void get_ShouldReturnCurrentDetail_WhenArticleExists() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        HttpResponse<String> created = browser.post(
                ARTICLES, editable("loaded-article", "Body"), token);
        UUID id = UUID.fromString(JSON.readTree(created.body()).get("id").asString());

        HttpResponse<String> response = browser.get(ARTICLES + "/" + id);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
        assertNoStore(response);
        assertDetail(JSON.readTree(response.body()), "loaded-article", "Body", 1);
    }

    @Test
    void update_ShouldRejectPublishedArticle_WhenSummaryIsRemoved() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createPublished(browser, token, "summary-removed");

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + id, payload("summary-removed", null, "# Body"), token, "\"2\"");

        assertProblem(response, 422, "CONTENT_SUMMARY_REQUIRED");
        assertThat(browser.get(ARTICLES + "/" + id).headers().firstValue("ETag")).contains("\"2\"");
    }

    @Test
    void update_ShouldRejectPublishedArticle_WhenBodyIsRemoved() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createPublished(browser, token, "body-removed");

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + id, payload("body-removed", "A short summary.", null), token, "\"2\"");

        assertProblem(response, 422, "ARTICLE_BODY_REQUIRED");
        assertThat(browser.get(ARTICLES + "/" + id).headers().firstValue("ETag")).contains("\"2\"");
    }

    @Test
    void update_ShouldAcceptPublishedArticle_WhenSummaryAndBodyStayPresent() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createPublished(browser, token, "still-complete");

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + id, payload("still-complete", "A new summary.", "# New body"), token, "\"2\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"3\"");
    }

    private String createPublished(Browser browser, String token, String slug) throws Exception {
        HttpResponse<String> created = browser.post(
                ARTICLES, payload(slug, "A short summary.", "# Body"), token);
        String id = JSON.readTree(created.body()).get("id").asString();
        HttpResponse<String> published = browser.post(
                "/api/admin/v1/content/" + id + "/publish", null, token, "\"1\"");
        assertThat(published.statusCode()).isEqualTo(200);
        return id;
    }

    private String payload(String slug, String summary, String bodyMarkdown) throws Exception {
        ObjectNode body = JSON.createObjectNode();
        body.put("title", "Published Article");
        body.put("slug", slug);
        if (summary == null) {
            body.putNull("summary");
        } else {
            body.put("summary", summary);
        }
        if (bodyMarkdown == null) {
            body.putNull("bodyMarkdown");
        } else {
            body.put("bodyMarkdown", bodyMarkdown);
        }
        body.putNull("moduleId");
        body.putNull("position");
        body.putArray("topicIds");
        return JSON.writeValueAsString(body);
    }

    private String editable(String slug, String bodyMarkdown) throws Exception {
        ObjectNode body = JSON.createObjectNode();
        body.put("title", "First Article");
        body.put("slug", slug);
        body.put("summary", "A short summary.");
        body.put("bodyMarkdown", bodyMarkdown);
        body.putNull("moduleId");
        body.putNull("position");
        body.putArray("topicIds");
        return JSON.writeValueAsString(body);
    }

    private static void assertDetail(
            JsonNode body, String slug, String bodyMarkdown, long revision) {
        assertThat(body.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "title", "slug", "summary", "bodyMarkdown",
                "moduleId", "position", "topicIds", "status", "revision",
                "publishedAt", "unpublishedAt", "createdAt", "updatedAt");
        assertThat(body.get("type").asString()).isEqualTo("article");
        assertThat(body.get("slug").asString()).isEqualTo(slug);
        assertThat(body.get("bodyMarkdown").asString()).isEqualTo(bodyMarkdown);
        assertThat(body.get("status").asString()).isEqualTo("DRAFT");
        assertThat(body.get("revision").asLong()).isEqualTo(revision);
        assertThat(body.get("moduleId").isNull()).isTrue();
        assertThat(body.get("position").isNull()).isTrue();
        assertThat(body.get("topicIds").isEmpty()).isTrue();
        assertThat(body.get("publishedAt").isNull()).isTrue();
        assertThat(body.get("unpublishedAt").isNull()).isTrue();
    }

    private long rowCount(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table, Long.class);
    }
}
