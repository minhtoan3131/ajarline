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
import vn.io.ajarline.support.AdminAuthenticationTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

/**
 * Runs one Article through the whole Admin to Reader lifecycle over HTTP.
 *
 * <p>Each step uses the session, CSRF token and ETag returned by the step
 * before it. Focused tests cover each operation in detail; this test checks
 * that the operations work together.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class
})
class ArticlePublicationFlowIT extends AbstractAdminAuthenticationIT {

    private static final String ADMIN_ARTICLES = "/api/admin/v1/articles";
    private static final String ADMIN_CONTENT = "/api/admin/v1/content";
    private static final String SLUG = "first-publication-flow";
    private static final String PUBLIC_ARTICLE = "/api/v1/articles/" + SLUG;
    private static final String DRAFT_TITLE = "First publication flow";
    private static final String EDITED_TITLE = "First publication flow, edited";

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void clearArticles() {
        jdbc.update("DELETE FROM content_item");
    }

    @Test
    void lifecycle_ShouldMoveArticleFromDraftToPublicAndBack_WhenAdminUsesCurrentRevision()
            throws Exception {
        // a signed-in Admin with the CSRF token of the new session
        Browser admin = new Browser();
        String anonymousToken = csrf(admin);
        String anonymousCookie = admin.cookie();
        assertJson(login(admin, anonymousToken,
                        AdminAuthenticationTestData.username(),
                        AdminAuthenticationTestData.password()),
                200, "{\"authenticated\":true}");
        assertThat(admin.cookie()).isNotEqualTo(anonymousCookie);
        String token = csrf(admin);
        assertThat(token).isNotEqualTo(anonymousToken);
        Browser reader = new Browser();

        // create a Draft at revision 1
        HttpResponse<String> created = admin.post(
                ADMIN_ARTICLES, articleJson(DRAFT_TITLE), token);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.headers().firstValue("ETag")).contains("\"1\"");
        assertNoStore(created);
        JsonNode draft = JSON.readTree(created.body());
        assertThat(draft.get("status").asString()).isEqualTo("DRAFT");
        assertThat(draft.get("revision").asLong()).isEqualTo(1);
        String id = draft.get("id").asString();
        String articlePath = ADMIN_ARTICLES + "/" + id;
        assertThat(created.headers().firstValue("Location"))
                .hasValueSatisfying(location -> assertThat(location).endsWith(articlePath));

        // a Draft is not public
        assertProblem(reader.get(PUBLIC_ARTICLE), 404, "CONTENT_NOT_FOUND");

        // an effective change moves revision 1 to 2
        HttpResponse<String> updated = admin.put(
                articlePath, articleJson(EDITED_TITLE), token, "\"1\"");
        assertRevision(updated, 2);

        // the same values again are a no-op and keep revision 2
        HttpResponse<String> unchanged = admin.put(
                articlePath, articleJson(EDITED_TITLE), token, "\"2\"");
        assertRevision(unchanged, 2);

        // a writer with the old revision cannot overwrite
        HttpResponse<String> stale = admin.put(
                articlePath, articleJson("Stale title"), token, "\"1\"");
        assertProblem(stale, 412, "REVISION_CONFLICT");
        HttpResponse<String> afterStale = admin.get(articlePath);
        assertRevision(afterStale, 2);
        assertThat(JSON.readTree(afterStale.body()).get("title").asString())
                .isEqualTo(EDITED_TITLE);

        // publish with the current revision
        HttpResponse<String> published = admin.post(
                ADMIN_CONTENT + "/" + id + "/publish", null, token, "\"2\"");
        assertPublication(published, "PUBLISHED", 3);

        // an anonymous reader sees the exact public Article
        HttpResponse<String> publicRead = reader.get(PUBLIC_ARTICLE);
        assertThat(publicRead.statusCode()).isEqualTo(200);
        assertPublicArticle(JSON.readTree(publicRead.body()), id);

        // unpublish with the current revision
        HttpResponse<String> unpublished = admin.post(
                ADMIN_CONTENT + "/" + id + "/unpublish", null, token, "\"3\"");
        assertPublication(unpublished, "UNPUBLISHED", 4);

        // the Article is no longer public
        assertProblem(reader.get(PUBLIC_ARTICLE), 404, "CONTENT_NOT_FOUND");

        // after logout the old session cannot read Admin data
        String sessionCookie = admin.cookie();
        HttpResponse<String> logout = admin.post(AUTH + "/logout", null, token);
        assertThat(logout.statusCode()).isEqualTo(204);
        assertProblem(new Browser(sessionCookie).get(articlePath),
                401, "AUTHENTICATION_REQUIRED");
    }

    private static String articleJson(String title) {
        ObjectNode request = JSON.createObjectNode();
        request.put("title", title);
        request.put("slug", SLUG);
        request.put("summary", "A short summary for the flow test.");
        request.put("bodyMarkdown", "# Heading\n\nBody text.");
        request.putNull("moduleId");
        request.putNull("position");
        request.putArray("topicIds");
        return JSON.writeValueAsString(request);
    }

    private static void assertRevision(HttpResponse<String> response, long revision) {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag"))
                .contains("\"" + revision + "\"");
        assertNoStore(response);
        assertThat(JSON.readTree(response.body()).get("revision").asLong())
                .isEqualTo(revision);
    }

    private static void assertPublication(
            HttpResponse<String> response, String status, long revision) {
        assertRevision(response, revision);
        assertThat(JSON.readTree(response.body()).get("status").asString())
                .isEqualTo(status);
    }

    private static void assertPublicArticle(JsonNode body, String id) {
        assertThat(body.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "slug", "title", "summary", "bodyMarkdown",
                "topics", "series", "module", "previousArticle",
                "nextArticle", "publishedAt", "updatedAt");
        assertThat(body.get("id").asString()).isEqualTo(id);
        assertThat(body.get("slug").asString()).isEqualTo(SLUG);
        assertThat(body.get("title").asString()).isEqualTo(EDITED_TITLE);
        assertThat(body.get("topics").isEmpty()).isTrue();
        assertThat(body.get("series").isNull()).isTrue();
        assertThat(body.get("module").isNull()).isTrue();
        assertThat(body.get("previousArticle").isNull()).isTrue();
        assertThat(body.get("nextArticle").isNull()).isTrue();
    }
}
