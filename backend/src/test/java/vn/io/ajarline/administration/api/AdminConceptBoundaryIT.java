package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.RetrievalTestData.deleteAllConcepts;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminConceptBoundaryIT extends AbstractAdminAuthenticationIT {

    private static final String CONCEPTS = "/api/admin/v1/concepts";
    private static final String MISSING_ID = "00000000-0000-0000-0000-000000000404";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearConcepts() {
        deleteAllConcepts(jdbcTemplate);
    }

    @Test
    void access_ShouldRejectRequests_WhenAdminSessionIsMissing() throws Exception {
        Browser browser = new Browser();
        String token = csrf(browser);

        HttpResponse<String> list = browser.get(CONCEPTS);
        HttpResponse<String> create = browser.post(CONCEPTS, create("anonymous-concept"), token);

        assertProblem(list, 401, "AUTHENTICATION_REQUIRED");
        assertProblem(create, 401, "AUTHENTICATION_REQUIRED");
        assertThat(rowCount()).isZero();
    }

    @Test
    void create_ShouldRejectRequest_WhenCsrfProofIsMissingOrWrong() throws Exception {
        Browser browser = authenticatedBrowser();
        csrf(browser);

        HttpResponse<String> missing = browser.post(CONCEPTS, create("missing-csrf"), null);
        HttpResponse<String> wrong = browser.post(CONCEPTS, create("wrong-csrf"), "invalid");

        assertProblem(missing, 403, "CSRF_INVALID");
        assertProblem(wrong, 403, "CSRF_INVALID");
        assertThat(rowCount()).isZero();
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
        wrongName.put("displayName", 7);
        ObjectNode wrongActive = createNode("wrong-active");
        wrongActive.put("active", "yes");
        ObjectNode absentDescription = createNode("absent-description");
        absentDescription.remove("description");
        ObjectNode absentExpressions = createNode("absent-expressions");
        absentExpressions.remove("expressions");
        ObjectNode expressionsNotArray = createNode("expressions-not-array");
        expressionsNotArray.put("expressions", "none");
        ObjectNode expressionNotObject = createNode("expression-not-object");
        expressionNotObject.putArray("expressions").add("thread");
        ObjectNode expressionMissingField = createNode("expression-missing-field");
        ObjectNode incomplete = expressionNode("thread");
        incomplete.remove("autoExpand");
        expressionMissingField.putArray("expressions").add(incomplete);
        ObjectNode expressionExtraField = createNode("expression-extra-field");
        ObjectNode extra = expressionNode("thread");
        extra.put("preferred", true);
        expressionExtraField.putArray("expressions").add(extra);
        ObjectNode hintNotText = createNode("hint-not-text");
        ObjectNode numericHint = expressionNode("thread");
        numericHint.put("languageHint", 7);
        hintNotText.putArray("expressions").add(numericHint);

        assertProblem(browser.post(CONCEPTS, "{", token), 400, "MALFORMED_JSON");
        assertProblem(browser.post(CONCEPTS, "[]", token), 400, "INVALID_REQUEST");
        for (ObjectNode body : new ObjectNode[]{
                missingField, extraField, wrongName, wrongActive, absentDescription,
                absentExpressions, expressionsNotArray, expressionNotObject,
                expressionMissingField, expressionExtraField, hintNotText}) {
            assertProblem(browser.post(CONCEPTS, JSON.writeValueAsString(body), token),
                    400, "INVALID_REQUEST");
        }
        assertProblem(browser.send("POST", CONCEPTS, create("plain-text"), token, "text/plain"),
                415, "UNSUPPORTED_MEDIA_TYPE");
        assertThat(rowCount()).isZero();
    }

    @Test
    void update_ShouldRejectRequest_WhenAdminSessionIsMissing() throws Exception {
        Browser browser = new Browser();
        String token = csrf(browser);

        HttpResponse<String> update = browser.put(
                CONCEPTS + "/" + MISSING_ID, edit("Name"), token, null);

        assertProblem(update, 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void update_ShouldRejectRequest_WhenCsrfProofIsMissingOrWrong() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createConcept(browser, token, "csrf-update");

        HttpResponse<String> missing = browser.put(CONCEPTS + "/" + id, edit("Changed"), null, null);
        HttpResponse<String> wrong = browser.put(CONCEPTS + "/" + id, edit("Changed"), "invalid", null);

        assertProblem(missing, 403, "CSRF_INVALID");
        assertProblem(wrong, 403, "CSRF_INVALID");
        assertThat(storedDisplayName(id)).isEqualTo("Original");
    }

    @Test
    void update_ShouldRejectRequest_WhenJsonShapeIsInvalid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createConcept(browser, token, "shape-update");
        ObjectNode missingField = editNode("Changed");
        missingField.remove("active");
        ObjectNode withKey = editNode("Changed");
        withKey.put("key", "new-key");
        ObjectNode absentExpressions = editNode("Changed");
        absentExpressions.remove("expressions");
        ObjectNode expressionNotObject = editNode("Changed");
        expressionNotObject.putArray("expressions").add("thread");

        assertProblem(browser.put(CONCEPTS + "/" + id, "{", token, null), 400, "MALFORMED_JSON");
        for (ObjectNode body : new ObjectNode[]{
                missingField, withKey, absentExpressions, expressionNotObject}) {
            assertProblem(browser.put(CONCEPTS + "/" + id, JSON.writeValueAsString(body), token, null),
                    400, "INVALID_REQUEST");
        }
        assertProblem(browser.send("PUT", CONCEPTS + "/" + id, edit("Changed"), token, "text/plain"),
                415, "UNSUPPORTED_MEDIA_TYPE");
        assertThat(storedDisplayName(id)).isEqualTo("Original");
    }

    @Test
    void update_ShouldRejectRequest_WhenIdIsNotUuid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        assertProblem(browser.put(CONCEPTS + "/not-a-uuid", edit("Name"), token, null),
                400, "INVALID_UUID");
    }

    private String createConcept(Browser browser, String token, String key) throws Exception {
        HttpResponse<String> response = browser.post(CONCEPTS, create(key), token);
        assertThat(response.statusCode()).isEqualTo(201);
        return JSON.readTree(response.body()).get("id").asString();
    }

    private ObjectNode editNode(String displayName) {
        ObjectNode body = JSON.createObjectNode();
        body.put("displayName", displayName);
        body.putNull("description");
        body.put("active", true);
        body.putArray("expressions").add(expressionNode("thread"));
        return body;
    }

    private String edit(String displayName) throws Exception {
        return JSON.writeValueAsString(editNode(displayName));
    }

    private String storedDisplayName(String id) {
        return jdbcTemplate.queryForObject(
                "SELECT display_name FROM retrieval_concept WHERE id = ?::uuid", String.class, id);
    }

    private ObjectNode expressionNode(String text) {
        ObjectNode node = JSON.createObjectNode();
        node.put("expression", text);
        node.put("languageHint", "en");
        node.put("autoExpand", true);
        return node;
    }

    private ObjectNode createNode(String key) {
        ObjectNode body = JSON.createObjectNode();
        body.put("key", key);
        body.put("displayName", "Original");
        body.putNull("description");
        body.put("active", true);
        ArrayNode expressions = body.putArray("expressions");
        expressions.add(expressionNode("thread"));
        return body;
    }

    private String create(String key) throws Exception {
        return JSON.writeValueAsString(createNode(key));
    }

    private long rowCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM retrieval_concept", Long.class);
    }
}
