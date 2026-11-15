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
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminTopicBoundaryIT extends AbstractAdminAuthenticationIT {

    private static final String TOPICS = "/api/admin/v1/topics";
    private static final String MISSING_ID = "00000000-0000-0000-0000-000000000404";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTopics() {
        jdbcTemplate.update("DELETE FROM content_item");
        jdbcTemplate.update("DELETE FROM topic");
    }

    @Test
    void access_ShouldRejectRequests_WhenAdminSessionIsMissing() throws Exception {
        Browser browser = new Browser();
        String token = csrf(browser);

        HttpResponse<String> list = browser.get(TOPICS);
        HttpResponse<String> create = browser.post(TOPICS, create("anonymous-topic"), token);
        HttpResponse<String> update = browser.put(
                TOPICS + "/" + MISSING_ID, edit("Name"), token, null);
        HttpResponse<String> delete = browser.send(
                "DELETE", TOPICS + "/" + MISSING_ID, null, token, null);

        assertProblem(list, 401, "AUTHENTICATION_REQUIRED");
        assertProblem(create, 401, "AUTHENTICATION_REQUIRED");
        assertProblem(update, 401, "AUTHENTICATION_REQUIRED");
        assertProblem(delete, 401, "AUTHENTICATION_REQUIRED");
        assertThat(rowCount()).isZero();
    }

    @Test
    void create_ShouldRejectRequest_WhenCsrfProofIsMissingOrWrong() throws Exception {
        Browser browser = authenticatedBrowser();
        csrf(browser);

        HttpResponse<String> missing = browser.post(TOPICS, create("missing-csrf"), null);
        HttpResponse<String> wrong = browser.post(TOPICS, create("wrong-csrf"), "invalid");

        assertProblem(missing, 403, "CSRF_INVALID");
        assertProblem(wrong, 403, "CSRF_INVALID");
        assertThat(rowCount()).isZero();
    }

    @Test
    void update_ShouldRejectRequest_WhenCsrfProofIsMissingOrWrong() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createTopic(browser, token, "csrf-update");

        HttpResponse<String> missing = browser.put(TOPICS + "/" + id, edit("Changed"), null, null);
        HttpResponse<String> wrong = browser.put(TOPICS + "/" + id, edit("Changed"), "invalid", null);

        assertProblem(missing, 403, "CSRF_INVALID");
        assertProblem(wrong, 403, "CSRF_INVALID");
        assertThat(storedName(id)).isEqualTo("Original");
    }

    @Test
    void delete_ShouldRejectRequest_WhenCsrfProofIsMissingOrWrong() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createTopic(browser, token, "csrf-delete");

        HttpResponse<String> missing = browser.send("DELETE", TOPICS + "/" + id, null, null, null);
        HttpResponse<String> wrong = browser.send("DELETE", TOPICS + "/" + id, null, "invalid", null);

        assertProblem(missing, 403, "CSRF_INVALID");
        assertProblem(wrong, 403, "CSRF_INVALID");
        assertThat(rowCount()).isEqualTo(1);
    }

    @Test
    void create_ShouldRejectRequest_WhenJsonShapeIsInvalid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        ObjectNode missingField = createNode("missing-field");
        missingField.remove("active");
        ObjectNode extraField = createNode("extra-field");
        extraField.put("unexpected", true);
        ObjectNode wrongName = createNode("wrong-name");
        wrongName.put("name", 7);
        ObjectNode wrongActive = createNode("wrong-active");
        wrongActive.put("active", "yes");
        ObjectNode absentDescription = createNode("absent-description");
        absentDescription.remove("description");

        assertProblem(browser.post(TOPICS, "{", token), 400, "MALFORMED_JSON");
        assertProblem(browser.post(TOPICS, "[]", token), 400, "INVALID_REQUEST");
        assertProblem(browser.post(TOPICS, JSON.writeValueAsString(missingField), token),
                400, "INVALID_REQUEST");
        assertProblem(browser.post(TOPICS, JSON.writeValueAsString(extraField), token),
                400, "INVALID_REQUEST");
        assertProblem(browser.post(TOPICS, JSON.writeValueAsString(wrongName), token),
                400, "INVALID_REQUEST");
        assertProblem(browser.post(TOPICS, JSON.writeValueAsString(wrongActive), token),
                400, "INVALID_REQUEST");
        assertProblem(browser.post(TOPICS, JSON.writeValueAsString(absentDescription), token),
                400, "INVALID_REQUEST");
        assertProblem(browser.send("POST", TOPICS, create("plain-text"), token, "text/plain"),
                415, "UNSUPPORTED_MEDIA_TYPE");
        assertThat(rowCount()).isZero();
    }

    @Test
    void update_ShouldRejectRequest_WhenJsonShapeIsInvalid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createTopic(browser, token, "shape-update");
        ObjectNode missingField = editNode("Changed");
        missingField.remove("active");
        ObjectNode withSlug = editNode("Changed");
        withSlug.put("slug", "new-slug");

        assertProblem(browser.put(TOPICS + "/" + id, "{", token, null), 400, "MALFORMED_JSON");
        assertProblem(browser.put(TOPICS + "/" + id, JSON.writeValueAsString(missingField), token, null),
                400, "INVALID_REQUEST");
        assertProblem(browser.put(TOPICS + "/" + id, JSON.writeValueAsString(withSlug), token, null),
                400, "INVALID_REQUEST");
        assertProblem(browser.send("PUT", TOPICS + "/" + id, edit("Changed"), token, "text/plain"),
                415, "UNSUPPORTED_MEDIA_TYPE");
        assertThat(storedName(id)).isEqualTo("Original");
    }

    @Test
    void access_ShouldRejectRequest_WhenIdIsNotUuid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        assertProblem(browser.put(TOPICS + "/not-a-uuid", edit("Name"), token, null),
                400, "INVALID_UUID");
        assertProblem(browser.send("DELETE", TOPICS + "/not-a-uuid", null, token, null),
                400, "INVALID_UUID");
    }

    private String createTopic(Browser browser, String token, String slug) throws Exception {
        HttpResponse<String> response = browser.post(TOPICS, create(slug), token);
        assertThat(response.statusCode()).isEqualTo(201);
        return JSON.readTree(response.body()).get("id").asString();
    }

    private ObjectNode createNode(String slug) {
        ObjectNode body = JSON.createObjectNode();
        body.put("slug", slug);
        body.put("name", "Original");
        body.putNull("description");
        body.put("active", true);
        return body;
    }

    private String create(String slug) throws Exception {
        return JSON.writeValueAsString(createNode(slug));
    }

    private ObjectNode editNode(String name) {
        ObjectNode body = JSON.createObjectNode();
        body.put("name", name);
        body.putNull("description");
        body.put("active", true);
        return body;
    }

    private String edit(String name) throws Exception {
        return JSON.writeValueAsString(editNode(name));
    }

    private String storedName(String id) {
        return jdbcTemplate.queryForObject(
                "SELECT name FROM topic WHERE id = ?::uuid", String.class, id);
    }

    private long rowCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM topic", Long.class);
    }
}
