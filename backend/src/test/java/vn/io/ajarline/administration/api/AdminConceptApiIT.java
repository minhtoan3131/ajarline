package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.administration.api.ConceptJsonFixture.assertExpression;
import static vn.io.ajarline.administration.api.ConceptJsonFixture.assertItem;
import static vn.io.ajarline.administration.api.ConceptJsonFixture.expression;
import static vn.io.ajarline.administration.api.ConceptJsonFixture.payload;
import static vn.io.ajarline.support.RetrievalTestData.currentVersion;
import static vn.io.ajarline.support.RetrievalTestData.deleteAllConcepts;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminConceptApiIT extends AbstractAdminAuthenticationIT {

    private static final String CONCEPTS = "/api/admin/v1/concepts";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearConcepts() {
        deleteAllConcepts(jdbcTemplate);
    }

    @Test
    void create_ShouldReturnConcept_WhenPayloadIsValid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(CONCEPTS, payload(
                "threading", "Threading", "Many tasks at once.", true,
                expression("Thread", "en", true), expression("đa luồng", null, false)), token);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        assertThat(response.headers().firstValue("ETag")).isEmpty();
        assertNoStore(response);
        JsonNode body = JSON.readTree(response.body());
        assertItem(body, "threading", "Threading", "Many tasks at once.", true);
        assertThat(response.headers().firstValue("Location"))
                .contains(CONCEPTS + "/" + body.get("id").asString());
        JsonNode expressions = body.get("expressions");
        assertThat(expressions).hasSize(2);
        assertExpression(expressions.get(0), "Thread", "en", true);
        assertExpression(expressions.get(1), "đa luồng", null, false);
        assertThat(rowCount()).isEqualTo(1);
    }

    @Test
    void create_ShouldNormalizeKeyAndStoreNullDescription_WhenValuesNeedNormalizing() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(CONCEPTS, payload(
                "  Multi-Threading ", "Threading", "", true), token);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("key").asString()).isEqualTo("multi-threading");
        assertThat(body.get("description").isNull()).isTrue();
        assertThat(body.get("expressions")).isEmpty();
    }

    @Test
    void create_ShouldRaiseVersionByOne_WhenConceptIsCreated() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        long before = currentVersion(jdbcTemplate);

        browser.post(CONCEPTS, payload("threading", "Threading", null, true), token);

        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before + 1);
    }

    @Test
    void create_ShouldRejectConcept_WhenKeyAlreadyExists() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        browser.post(CONCEPTS, payload("same-key", "First", null, true), token);
        long before = currentVersion(jdbcTemplate);

        HttpResponse<String> response = browser.post(
                CONCEPTS, payload("same-key", "Second", null, true), token);

        assertProblem(response, 409, "CONCEPT_KEY_CONFLICT");
        assertThat(rowCount()).isEqualTo(1);
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void create_ShouldRejectConcept_WhenValuesAreInvalid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        assertProblem(browser.post(CONCEPTS, payload("Not a key", "Name", null, true), token),
                422, "VALIDATION_FAILED");
        assertProblem(browser.post(CONCEPTS, payload("blank-name", "   ", null, true), token),
                422, "VALIDATION_FAILED");
        assertProblem(browser.post(CONCEPTS, payload("bad-hint", "Name", null, true,
                expression("Thread", "fr", true)), token), 422, "VALIDATION_FAILED");
        assertProblem(browser.post(CONCEPTS, payload("blank-text", "Name", null, true,
                expression("   ", "en", true)), token), 422, "VALIDATION_FAILED");
        assertThat(rowCount()).isZero();
    }

    @Test
    void create_ShouldRejectConcept_WhenExpressionsRepeatAfterNormalization() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(CONCEPTS, payload(
                "threading", "Threading", null, true,
                expression("Thread", "en", true), expression(" thread ", "vi", false)), token);

        assertProblem(response, 422, "DUPLICATE_CONCEPT_EXPRESSION");
        assertThat(rowCount()).isZero();
    }

    @Test
    void create_ShouldAcceptSameExpression_WhenItIsInTwoConcepts() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> first = browser.post(CONCEPTS, payload(
                "threading", "Threading", null, true, expression("Thread", "en", true)), token);
        HttpResponse<String> second = browser.post(CONCEPTS, payload(
                "execution", "Execution", null, true, expression("thread", "en", true)), token);

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(second.statusCode()).isEqualTo(201);
    }

    @Test
    void list_ShouldReturnEmptyArray_WhenNoConceptExists() throws Exception {
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.get(CONCEPTS);

        assertThat(response.statusCode()).isEqualTo(200);
        assertNoStore(response);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames()).containsExactly("concepts");
        assertThat(body.get("concepts").isArray()).isTrue();
        assertThat(body.get("concepts")).isEmpty();
    }

    @Test
    void list_ShouldReturnActiveAndInactiveConceptsOrderedByName_WhenConceptsExist() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        browser.post(CONCEPTS, payload("gamma", "Gamma", null, true), token);
        browser.post(CONCEPTS, payload("alpha", "Alpha", "Inactive one.", false,
                expression("Second", "en", true), expression("first", "vi", false)), token);
        browser.post(CONCEPTS, payload("beta", "Beta", null, true), token);

        HttpResponse<String> response = browser.get(CONCEPTS);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode concepts = JSON.readTree(response.body()).get("concepts");
        assertThat(concepts).hasSize(3);
        assertItem(concepts.get(0), "alpha", "Alpha", "Inactive one.", false);
        assertItem(concepts.get(1), "beta", "Beta", null, true);
        assertItem(concepts.get(2), "gamma", "Gamma", null, true);
        JsonNode expressions = concepts.get(0).get("expressions");
        assertExpression(expressions.get(0), "first", "vi", false);
        assertExpression(expressions.get(1), "Second", "en", true);
    }

    private long rowCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM retrieval_concept", Long.class);
    }
}
