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
class AdminArticleRevisionIT extends AbstractAdminAuthenticationIT {

    private static final String ARTICLES = "/api/admin/v1/articles";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearArticles() {
        jdbcTemplate.update("DELETE FROM content_item");
    }

    @Test
    void update_ShouldReturnRevisionTwo_WhenContentChanges() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        UUID id = create(browser, token, "revision-article", "Old body");

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + id,
                editable("revision-article", "New body"),
                token,
                "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"2\"");
        assertNoStore(response);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("revision").asLong()).isEqualTo(2);
        assertThat(body.get("bodyMarkdown").asString()).isEqualTo("New body");
    }

    @Test
    void update_ShouldKeepRevisionAndTime_WhenPayloadIsEffectiveNoOp() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        UUID id = create(browser, token, "no-op-article", "Same body");
        JsonNode before = JSON.readTree(browser.get(ARTICLES + "/" + id).body());

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + id,
                editable("no-op-article", "Same body"),
                token,
                "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
        JsonNode after = JSON.readTree(response.body());
        assertThat(after.get("revision").asLong()).isEqualTo(1);
        assertThat(after.get("updatedAt")).isEqualTo(before.get("updatedAt"));
    }

    @Test
    void update_ShouldRequirePrecondition_WhenIfMatchIsMissing() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        UUID id = create(browser, token, "missing-header", "Body");

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + id,
                editable("missing-header", "Changed"),
                token,
                null);

        assertProblem(response, 428, "REVISION_PRECONDITION_REQUIRED");
        assertThat(JSON.readTree(browser.get(ARTICLES + "/" + id).body())
                .get("bodyMarkdown").asString()).isEqualTo("Body");
    }

    @Test
    void update_ShouldRejectRequest_WhenIfMatchIsMalformed() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        UUID id = create(browser, token, "invalid-header", "Body");

        for (String ifMatch : new String[]{
                "*", "W/\"1\"", "1", "\"1\", \"2\"", "\"0\"",
                "\"9223372036854775808\""}) {
            HttpResponse<String> response = browser.put(
                    ARTICLES + "/" + id,
                    editable("invalid-header", "Changed"),
                    token,
                    ifMatch);
            assertProblem(response, 400, "INVALID_REQUEST");
        }
        assertThat(JSON.readTree(browser.get(ARTICLES + "/" + id).body())
                .get("revision").asLong()).isEqualTo(1);
    }

    @Test
    void update_ShouldPreserveWinnerState_WhenIfMatchIsStale() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        UUID id = create(browser, token, "stale-article", "Original");
        HttpResponse<String> winner = browser.put(
                ARTICLES + "/" + id,
                editable("stale-article", "Winner"),
                token,
                "\"1\"");
        assertThat(winner.statusCode()).isEqualTo(200);

        HttpResponse<String> loser = browser.put(
                ARTICLES + "/" + id,
                editable("stale-article", "Loser"),
                token,
                "\"1\"");

        assertProblem(loser, 412, "REVISION_CONFLICT");
        HttpResponse<String> stored = browser.get(ARTICLES + "/" + id);
        assertThat(stored.headers().firstValue("ETag")).contains("\"2\"");
        assertThat(JSON.readTree(stored.body()).get("bodyMarkdown").asString())
                .isEqualTo("Winner");
    }

    private UUID create(
            Browser browser, String token, String slug, String body) throws Exception {
        HttpResponse<String> response = browser.post(
                ARTICLES, editable(slug, body), token);
        assertThat(response.statusCode()).isEqualTo(201);
        return UUID.fromString(JSON.readTree(response.body()).get("id").asString());
    }

    private String editable(String slug, String bodyMarkdown) throws Exception {
        ObjectNode body = JSON.createObjectNode();
        body.put("title", "Revision Article");
        body.put("slug", slug);
        body.put("summary", "A short summary.");
        body.put("bodyMarkdown", bodyMarkdown);
        body.putNull("moduleId");
        body.putNull("position");
        body.putArray("topicIds");
        return JSON.writeValueAsString(body);
    }
}
