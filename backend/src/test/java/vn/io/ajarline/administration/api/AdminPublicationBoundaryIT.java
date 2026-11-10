package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.node.ObjectNode;
import vn.io.ajarline.support.AdminAuthenticationTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class
})
class AdminPublicationBoundaryIT extends AbstractAdminAuthenticationIT {

    private static final String ARTICLES = "/api/admin/v1/articles";
    private static final String CONTENT = "/api/admin/v1/content";
    private static final String MISSING =
            "00000000-0000-0000-0000-000000000404";

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void clearArticles() {
        jdbc.update("DELETE FROM content_item");
    }

    @Test
    void publish_ShouldRequireSessionAndCurrentCsrf() throws Exception {
        Browser anonymous = new Browser();
        String anonymousToken = csrf(anonymous);
        assertProblem(anonymous.post(
                        CONTENT + "/" + MISSING + "/publish",
                        null, anonymousToken, "\"1\""),
                401, "AUTHENTICATION_REQUIRED");

        Browser admin = authenticatedBrowser();
        csrf(admin);
        assertProblem(admin.post(
                        CONTENT + "/" + MISSING + "/publish",
                        null, null, "\"1\""),
                403, "CSRF_INVALID");
        assertProblem(admin.post(
                        CONTENT + "/" + MISSING + "/publish",
                        null, "wrong", "\"1\""),
                403, "CSRF_INVALID");

        Browser rotating = new Browser();
        String oldToken = csrf(rotating);
        assertThat(login(
                rotating,
                oldToken,
                AdminAuthenticationTestData.username(),
                AdminAuthenticationTestData.password()).statusCode())
                .isEqualTo(200);
        assertProblem(rotating.post(
                        CONTENT + "/" + MISSING + "/publish",
                        null, oldToken, "\"1\""),
                403, "CSRF_INVALID");
    }

    @Test
    void publish_ShouldEnforceIfMatchBeforeMutation() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = create(browser, token);

        assertProblem(browser.post(
                        CONTENT + "/" + id + "/publish",
                        null, token, null),
                428, "REVISION_PRECONDITION_REQUIRED");

        for (String value : new String[]{
                "*", "W/\"1\"", "1", "\"1\", \"2\"", "\"0\""
        }) {
            assertProblem(browser.post(
                            CONTENT + "/" + id + "/publish",
                            null, token, value),
                    400, "INVALID_REQUEST");
        }

        HttpResponse<String> winner = browser.post(
                CONTENT + "/" + id + "/publish",
                null, token, "\"1\"");
        assertThat(winner.statusCode()).isEqualTo(200);

        assertProblem(browser.post(
                        CONTENT + "/" + id + "/publish",
                        null, token, "\"1\""),
                412, "REVISION_CONFLICT");
    }

    @Test
    void unpublish_ShouldRequireCurrentCsrfAndRevision() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = create(browser, token);
        assertThat(browser.post(
                CONTENT + "/" + id + "/publish",
                null, token, "\"1\"").statusCode()).isEqualTo(200);

        assertProblem(browser.post(
                        CONTENT + "/" + id + "/unpublish",
                        null, null, "\"2\""),
                403, "CSRF_INVALID");
        assertProblem(browser.post(
                        CONTENT + "/" + id + "/unpublish",
                        null, token, null),
                428, "REVISION_PRECONDITION_REQUIRED");
        assertProblem(browser.post(
                        CONTENT + "/" + id + "/unpublish",
                        null, token, "W/\"2\""),
                400, "INVALID_REQUEST");
        assertProblem(browser.post(
                        CONTENT + "/" + id + "/unpublish",
                        null, token, "\"1\""),
                412, "REVISION_CONFLICT");

        assertThat(browser.post(
                CONTENT + "/" + id + "/unpublish",
                null, token, "\"2\"").statusCode()).isEqualTo(200);
    }

    @Test
    void action_ShouldRejectBodyAndInvalidIdentifier() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        assertProblem(browser.post(
                        CONTENT + "/" + MISSING + "/publish",
                        "{}", token, "\"1\""),
                400, "INVALID_REQUEST");
        assertProblem(browser.post(
                        CONTENT + "/not-a-uuid/publish",
                        null, token, "\"1\""),
                400, "INVALID_UUID");
        assertProblem(browser.post(
                        CONTENT + "/" + MISSING + "/publish",
                        null, token, "\"1\""),
                404, "CONTENT_NOT_FOUND");
    }

    private String create(Browser browser, String token) throws Exception {
        ObjectNode request = JSON.createObjectNode();
        request.put("title", "Boundary Article");
        request.put("slug", "publication-boundary");
        request.put("summary", "Summary");
        request.put("bodyMarkdown", "# Body");
        request.putNull("moduleId");
        request.putNull("position");
        request.putArray("topicIds");

        HttpResponse<String> response = browser.post(
                ARTICLES, JSON.writeValueAsString(request), token);
        assertThat(response.statusCode()).isEqualTo(201);
        return JSON.readTree(response.body()).get("id").asString();
    }
}
