package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
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
@Import({
        PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class
})
class AdminPublicationApiIT extends AbstractAdminAuthenticationIT {

    private static final String ARTICLES = "/api/admin/v1/articles";
    private static final String CONTENT = "/api/admin/v1/content";

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void clearArticles() {
        jdbc.update("DELETE FROM content_item");
    }

    @Test
    void lifecycle_ShouldReturnContractStateAndEtag() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = create(browser, token, "publication-api",
                "Summary", "# Body");

        HttpResponse<String> published = browser.post(
                CONTENT + "/" + id + "/publish", null, token, "\"1\"");
        assertPublication(published, "PUBLISHED", 2);
        JsonNode publishedBody = JSON.readTree(published.body());
        assertThat(publishedBody.get("publishedAt").isNull()).isFalse();
        assertThat(publishedBody.get("unpublishedAt").isNull()).isTrue();

        HttpResponse<String> unpublished = browser.post(
                CONTENT + "/" + id + "/unpublish", null, token, "\"2\"");
        assertPublication(unpublished, "UNPUBLISHED", 3);
        JsonNode unpublishedBody = JSON.readTree(unpublished.body());
        assertThat(unpublishedBody.get("publishedAt"))
                .isEqualTo(publishedBody.get("publishedAt"));
        assertThat(unpublishedBody.get("unpublishedAt").isNull()).isFalse();

        HttpResponse<String> unchangedUnpublished = browser.post(
                CONTENT + "/" + id + "/unpublish", null, token, "\"3\"");
        assertPublication(unchangedUnpublished, "UNPUBLISHED", 3);
        assertThat(JSON.readTree(unchangedUnpublished.body()))
                .isEqualTo(unpublishedBody);

        HttpResponse<String> republished = browser.post(
                CONTENT + "/" + id + "/publish", null, token, "\"3\"");
        assertPublication(republished, "PUBLISHED", 4);
        JsonNode republishedBody = JSON.readTree(republished.body());
        assertThat(republishedBody.get("publishedAt"))
                .isEqualTo(publishedBody.get("publishedAt"));
        assertThat(republishedBody.get("unpublishedAt"))
                .isEqualTo(unpublishedBody.get("unpublishedAt"));
    }

    @Test
    void action_ShouldKeepRevision_WhenTransitionIsIdempotent()
            throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = create(browser, token, "idempotent-api",
                "Summary", "# Body");
        HttpResponse<String> first = browser.post(
                CONTENT + "/" + id + "/publish", null, token, "\"1\"");
        JsonNode before = JSON.readTree(first.body());

        HttpResponse<String> second = browser.post(
                CONTENT + "/" + id + "/publish", null, token, "\"2\"");

        assertPublication(second, "PUBLISHED", 2);
        JsonNode after = JSON.readTree(second.body());
        assertThat(after.get("updatedAt")).isEqualTo(before.get("updatedAt"));
        assertThat(after.get("publishedAt")).isEqualTo(before.get("publishedAt"));
    }

    @Test
    void publish_ShouldReturnSpecificValidationCode_WhenDraftIsIncomplete()
            throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String noSummary = create(
                browser, token, "no-summary-api", null, "# Body");
        String noBody = create(
                browser, token, "no-body-api", "Summary", null);

        assertProblem(browser.post(
                        CONTENT + "/" + noSummary + "/publish",
                        null, token, "\"1\""),
                422, "CONTENT_SUMMARY_REQUIRED");
        assertProblem(browser.post(
                        CONTENT + "/" + noBody + "/publish",
                        null, token, "\"1\""),
                422, "ARTICLE_BODY_REQUIRED");
    }

    @Test
    void unpublish_ShouldRejectDraftWithoutChangingRevision()
            throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = create(
                browser, token, "draft-unpublish-api", "Summary", "# Body");

        assertProblem(browser.post(
                        CONTENT + "/" + id + "/unpublish",
                        null, token, "\"1\""),
                409, "INVALID_PUBLICATION_STATE");
        assertThat(browser.get(ARTICLES + "/" + id)
                .headers().firstValue("ETag")).contains("\"1\"");
    }

    private String create(
            Browser browser,
            String token,
            String slug,
            String summary,
            String bodyMarkdown
    ) throws Exception {
        ObjectNode request = JSON.createObjectNode();
        request.put("title", "Publication API Article");
        request.put("slug", slug);
        if (summary == null) {
            request.putNull("summary");
        } else {
            request.put("summary", summary);
        }
        if (bodyMarkdown == null) {
            request.putNull("bodyMarkdown");
        } else {
            request.put("bodyMarkdown", bodyMarkdown);
        }
        request.putNull("moduleId");
        request.putNull("position");
        request.putArray("topicIds");

        HttpResponse<String> response = browser.post(
                ARTICLES, JSON.writeValueAsString(request), token);
        assertThat(response.statusCode()).isEqualTo(201);
        return JSON.readTree(response.body()).get("id").asString();
    }

    private static void assertPublication(
            HttpResponse<String> response,
            String status,
            long revision
    ) {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        assertThat(response.headers().firstValue("ETag"))
                .contains("\"" + revision + "\"");
        assertNoStore(response);

        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "status", "revision",
                "publishedAt", "unpublishedAt", "updatedAt");
        assertThat(body.get("type").asString()).isEqualTo("article");
        assertThat(body.get("status").asString()).isEqualTo(status);
        assertThat(body.get("revision").asLong()).isEqualTo(revision);
    }
}
