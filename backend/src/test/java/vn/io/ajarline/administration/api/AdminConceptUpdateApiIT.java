package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.administration.api.ConceptJsonFixture.assertExpression;
import static vn.io.ajarline.administration.api.ConceptJsonFixture.assertItem;
import static vn.io.ajarline.administration.api.ConceptJsonFixture.editPayload;
import static vn.io.ajarline.administration.api.ConceptJsonFixture.expression;
import static vn.io.ajarline.administration.api.ConceptJsonFixture.payload;
import static vn.io.ajarline.support.RetrievalTestData.currentVersion;
import static vn.io.ajarline.support.RetrievalTestData.deleteAllConcepts;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
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
class AdminConceptUpdateApiIT extends AbstractAdminAuthenticationIT {

    private static final String CONCEPTS = "/api/admin/v1/concepts";
    private static final String MISSING_ID = "00000000-0000-0000-0000-000000000404";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearConcepts() {
        deleteAllConcepts(jdbcTemplate);
    }

    @Test
    void update_ShouldReplaceValuesAndExpressions_WhenPayloadIsValid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createConcept(browser, token, "threading", expression("Thread", "en", true));
        long before = currentVersion(jdbcTemplate);

        HttpResponse<String> response = browser.put(CONCEPTS + "/" + id, editPayload(
                "Threads", "Many tasks.", false,
                expression("Thread", "en", true), expression("Task", "en", false)), token, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        assertThat(response.headers().firstValue("ETag")).isEmpty();
        assertNoStore(response);
        JsonNode body = JSON.readTree(response.body());
        assertItem(body, "threading", "Threads", "Many tasks.", false);
        assertThat(body.get("id").asString()).isEqualTo(id);
        assertThat(body.get("expressions")).hasSize(2);
        assertExpression(body.get("expressions").get(0), "Task", "en", false);
        assertExpression(body.get("expressions").get(1), "Thread", "en", true);
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before + 1);
    }

    @Test
    void update_ShouldReturnConceptUnchanged_WhenNothingDiffers() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createConcept(browser, token, "threading", expression("Thread", "en", true));
        long before = currentVersion(jdbcTemplate);
        Instant updatedAtBefore = storedUpdatedAt(id);

        HttpResponse<String> response = browser.put(CONCEPTS + "/" + id, editPayload(
                "Threading", null, true, expression("Thread", "en", true)), token, null);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(response.body());
        assertItem(body, "threading", "Threading", null, true);
        assertThat(Instant.parse(body.get("updatedAt").asString())).isEqualTo(updatedAtBefore);
        assertThat(storedUpdatedAt(id)).isEqualTo(updatedAtBefore);
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void update_ShouldReturnConceptUnchanged_WhenExpressionsComeInAnotherOrder() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createConcept(browser, token, "threading",
                expression("Thread", "en", true), expression("Task", "en", false));
        long before = currentVersion(jdbcTemplate);

        HttpResponse<String> response = browser.put(CONCEPTS + "/" + id, editPayload(
                "Threading", null, true,
                expression("Task", "en", false), expression("Thread", "en", true)), token, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void update_ShouldRaiseVersionByOne_WhenOnlyAutoExpandChanges() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createConcept(browser, token, "threading", expression("Thread", "en", true));
        long before = currentVersion(jdbcTemplate);

        HttpResponse<String> response = browser.put(CONCEPTS + "/" + id, editPayload(
                "Threading", null, true, expression("Thread", "en", false)), token, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertExpression(JSON.readTree(response.body()).get("expressions").get(0), "Thread", "en", false);
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before + 1);
    }

    @Test
    void update_ShouldRemoveAllExpressions_WhenSetIsEmpty() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createConcept(browser, token, "threading", expression("Thread", "en", true));

        HttpResponse<String> response = browser.put(
                CONCEPTS + "/" + id, editPayload("Threading", null, true), token, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(response.body()).get("expressions")).isEmpty();
    }

    @Test
    void update_ShouldRejectConcept_WhenExpressionsRepeatAfterNormalization() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createConcept(browser, token, "threading", expression("Thread", "en", true));
        long before = currentVersion(jdbcTemplate);

        HttpResponse<String> response = browser.put(CONCEPTS + "/" + id, editPayload(
                "Changed", null, true,
                expression("Task", "en", true), expression(" TASK ", "vi", false)), token, null);

        assertProblem(response, 422, "DUPLICATE_CONCEPT_EXPRESSION");
        assertThat(storedDisplayName(id)).isEqualTo("Threading");
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void update_ShouldRejectConcept_WhenValuesAreInvalid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createConcept(browser, token, "threading");
        ObjectNode[] tooMany = new ObjectNode[51];
        for (int index = 0; index < tooMany.length; index++) {
            tooMany[index] = expression("expression " + index, "en", true);
        }

        assertProblem(browser.put(CONCEPTS + "/" + id,
                editPayload("   ", null, true), token, null), 422, "VALIDATION_FAILED");
        assertProblem(browser.put(CONCEPTS + "/" + id,
                editPayload("Name", null, true, expression("Thread", "fr", true)), token, null),
                422, "VALIDATION_FAILED");
        assertProblem(browser.put(CONCEPTS + "/" + id,
                editPayload("Name", null, true, tooMany), token, null), 422, "VALIDATION_FAILED");
        assertThat(storedDisplayName(id)).isEqualTo("Threading");
    }

    @Test
    void update_ShouldReturnNotFound_WhenConceptDoesNotExist() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        long before = currentVersion(jdbcTemplate);

        HttpResponse<String> response = browser.put(
                CONCEPTS + "/" + MISSING_ID, editPayload("Name", null, true), token, null);

        assertProblem(response, 404, "CONCEPT_NOT_FOUND");
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void update_ShouldReportValidationFailureFirst_WhenIdIsUnknownAndInputIsInvalid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                CONCEPTS + "/" + MISSING_ID, editPayload("   ", null, true), token, null);

        assertProblem(response, 422, "VALIDATION_FAILED");
    }

    private String createConcept(
            Browser browser, String token, String key, ObjectNode... expressions) throws Exception {
        HttpResponse<String> response = browser.post(
                CONCEPTS, payload(key, "Threading", null, true, expressions), token);
        assertThat(response.statusCode()).isEqualTo(201);
        return JSON.readTree(response.body()).get("id").asString();
    }

    private String storedDisplayName(String id) {
        return jdbcTemplate.queryForObject(
                "SELECT display_name FROM retrieval_concept WHERE id = ?::uuid", String.class, id);
    }

    private Instant storedUpdatedAt(String id) {
        return jdbcTemplate.queryForObject(
                "SELECT updated_at FROM retrieval_concept WHERE id = ?::uuid",
                OffsetDateTime.class, id).toInstant();
    }
}
