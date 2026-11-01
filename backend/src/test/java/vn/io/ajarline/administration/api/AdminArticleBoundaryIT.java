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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminArticleBoundaryIT extends AbstractAdminAuthenticationIT {

    private static final String ARTICLES = "/api/admin/v1/articles";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearArticles() {
        jdbcTemplate.update("DELETE FROM content_item");
    }

    @Test
    void access_ShouldRejectRequests_WhenAdminSessionIsMissing() throws Exception {
        Browser browser = new Browser();
        String token = csrf(browser);
        String missingId = "00000000-0000-0000-0000-000000000404";

        HttpResponse<String> get = browser.get(ARTICLES + "/" + missingId);
        HttpResponse<String> create = browser.post(
                ARTICLES, standalone("anonymous-article", "Body"), token);
        HttpResponse<String> update = browser.put(
                ARTICLES + "/" + missingId,
                standalone("anonymous-article", "Body"),
                token,
                "\"1\"");

        assertProblem(get, 401, "AUTHENTICATION_REQUIRED");
        assertProblem(create, 401, "AUTHENTICATION_REQUIRED");
        assertProblem(update, 401, "AUTHENTICATION_REQUIRED");
        assertThat(rowCount()).isZero();
    }

    @Test
    void create_ShouldRejectRequest_WhenCsrfProofIsMissingWrongOrRotated() throws Exception {
        Browser browser = authenticatedBrowser();
        csrf(browser);

        Browser rotating = new Browser();
        String oldToken = csrf(rotating);
        HttpResponse<String> login = login(
                rotating,
                oldToken,
                AdminAuthenticationTestData.username(),
                AdminAuthenticationTestData.password());
        assertThat(login.statusCode()).isEqualTo(200);

        HttpResponse<String> missing = browser.post(
                ARTICLES, standalone("missing-csrf", "Body"), null);
        HttpResponse<String> wrong = browser.post(
                ARTICLES, standalone("wrong-csrf", "Body"), "invalid");
        HttpResponse<String> rotated = rotating.post(
                ARTICLES, standalone("rotated-csrf", "Body"), oldToken);

        assertProblem(missing, 403, "CSRF_INVALID");
        assertProblem(wrong, 403, "CSRF_INVALID");
        assertProblem(rotated, 403, "CSRF_INVALID");
        assertThat(rowCount()).isZero();
    }

    @Test
    void update_ShouldRejectRequest_WhenCsrfProofIsMissingWrongOrRotated() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        HttpResponse<String> created = browser.post(
                ARTICLES, standalone("csrf-update", "Original"), token);
        String id = JSON.readTree(created.body()).get("id").asString();

        Browser rotating = new Browser();
        String oldToken = csrf(rotating);
        assertThat(login(
                rotating,
                oldToken,
                AdminAuthenticationTestData.username(),
                AdminAuthenticationTestData.password()).statusCode()).isEqualTo(200);

        HttpResponse<String> missing = browser.put(
                ARTICLES + "/" + id,
                standalone("csrf-update", "Missing"),
                null,
                "\"1\"");
        HttpResponse<String> wrong = browser.put(
                ARTICLES + "/" + id,
                standalone("csrf-update", "Wrong"),
                "invalid",
                "\"1\"");
        HttpResponse<String> rotated = rotating.put(
                ARTICLES + "/" + id,
                standalone("csrf-update", "Rotated"),
                oldToken,
                "\"1\"");

        assertProblem(missing, 403, "CSRF_INVALID");
        assertProblem(wrong, 403, "CSRF_INVALID");
        assertProblem(rotated, 403, "CSRF_INVALID");
        HttpResponse<String> stored = browser.get(ARTICLES + "/" + id);
        assertThat(stored.headers().firstValue("ETag")).contains("\"1\"");
        assertThat(JSON.readTree(stored.body()).get("bodyMarkdown").asString())
                .isEqualTo("Original");
    }

    @Test
    void create_ShouldRejectRequest_WhenJsonShapeIsInvalid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        ObjectNode missingField = standaloneNode("missing-field", "Body");
        missingField.remove("position");
        ObjectNode extraField = standaloneNode("extra-field", "Body");
        extraField.put("unexpected", true);

        assertProblem(browser.post(ARTICLES, "{", token),
                400, "MALFORMED_JSON");
        assertProblem(browser.post(
                        ARTICLES, JSON.writeValueAsString(missingField), token),
                400, "INVALID_REQUEST");
        assertProblem(browser.post(
                        ARTICLES, JSON.writeValueAsString(extraField), token),
                400, "INVALID_REQUEST");
        assertProblem(browser.send(
                        "POST", ARTICLES, standalone("plain-text", "Body"),
                        token, "text/plain"),
                415, "UNSUPPORTED_MEDIA_TYPE");
        assertThat(rowCount()).isZero();
    }

    @Test
    void create_ShouldRejectRequest_WhenModulePositionPairIsIncomplete() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        ObjectNode invalidPair = standaloneNode("invalid-pair", "Body");
        invalidPair.put("position", 1);

        assertProblem(browser.post(
                        ARTICLES, JSON.writeValueAsString(invalidPair), token),
                422, "INVALID_POSITION");
        assertThat(rowCount()).isZero();
    }

    @Test
    void request_ShouldReturnCanonicalProblem_WhenContentFails() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        HttpResponse<String> first = browser.post(
                ARTICLES, standalone("same-slug", "Body"), token);
        assertThat(first.statusCode()).isEqualTo(201);

        HttpResponse<String> duplicate = browser.post(
                ARTICLES, standalone("same-slug", "Other"), token);
        ObjectNode invalid = standaloneNode("invalid-title", "Body");
        invalid.put("title", " ");
        HttpResponse<String> validation = browser.post(
                ARTICLES, JSON.writeValueAsString(invalid), token);
        HttpResponse<String> missing = browser.get(
                ARTICLES + "/00000000-0000-0000-0000-000000000404");
        HttpResponse<String> invalidUuid = browser.get(ARTICLES + "/not-a-uuid");

        assertProblem(duplicate, 409, "SLUG_CONFLICT");
        assertProblem(validation, 422, "VALIDATION_FAILED");
        assertProblem(missing, 404, "CONTENT_NOT_FOUND");
        assertProblem(invalidUuid, 400, "INVALID_UUID");
        assertThat(rowCount()).isEqualTo(1);
    }

    private String standalone(String slug, String body) throws Exception {
        return JSON.writeValueAsString(standaloneNode(slug, body));
    }

    private ObjectNode standaloneNode(String slug, String body) {
        ObjectNode request = JSON.createObjectNode();
        request.put("title", "Boundary Article");
        request.put("slug", slug);
        request.put("summary", "A short summary.");
        request.put("bodyMarkdown", body);
        request.putNull("moduleId");
        request.putNull("position");
        request.putArray("topicIds");
        return request;
    }

    private long rowCount() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM content_item", Long.class);
    }
}
