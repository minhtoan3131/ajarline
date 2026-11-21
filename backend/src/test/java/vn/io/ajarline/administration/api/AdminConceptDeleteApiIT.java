package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
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
import tools.jackson.databind.node.ObjectNode;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminConceptDeleteApiIT extends AbstractAdminAuthenticationIT {

    private static final String CONCEPTS = "/api/admin/v1/concepts";
    private static final String MISSING_ID = "00000000-0000-0000-0000-000000000404";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearConcepts() {
        deleteAllConcepts(jdbcTemplate);
    }

    @Test
    void delete_ShouldDeleteConceptAndItsExpressions_WhenConceptExists() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String doomed = createConcept(browser, token, "doomed",
                expression("Thread", "en", true), expression("Task", "en", false));
        createConcept(browser, token, "survivor", expression("Process", "en", true));
        long before = currentVersion(jdbcTemplate);

        HttpResponse<String> response = browser.send("DELETE", CONCEPTS + "/" + doomed, null, token, null);

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
        assertThat(response.headers().firstValue("ETag")).isEmpty();
        assertNoStore(response);
        assertThat(rowCount("retrieval_concept")).isEqualTo(1);
        assertThat(rowCount("retrieval_concept_expression")).isEqualTo(1);
        assertThat(expressionsOf(doomed)).isZero();
        JsonNode concepts = JSON.readTree(browser.get(CONCEPTS).body()).get("concepts");
        assertThat(concepts).hasSize(1);
        assertThat(concepts.get(0).get("key").asString()).isEqualTo("survivor");
        assertThat(concepts.get(0).get("expressions")).hasSize(1);
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before + 1);
    }

    @Test
    void delete_ShouldFreeTheKey_WhenConceptIsDeleted() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createConcept(browser, token, "reusable-key");

        browser.send("DELETE", CONCEPTS + "/" + id, null, token, null);
        HttpResponse<String> again = browser.post(
                CONCEPTS, payload("reusable-key", "Again", null, true), token);

        assertThat(again.statusCode()).isEqualTo(201);
    }

    @Test
    void delete_ShouldReturnNotFound_WhenConceptDoesNotExist() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        long before = currentVersion(jdbcTemplate);

        HttpResponse<String> response = browser.send(
                "DELETE", CONCEPTS + "/" + MISSING_ID, null, token, null);

        assertProblem(response, 404, "CONCEPT_NOT_FOUND");
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void delete_ShouldReturnNotFound_WhenConceptIsDeletedTwice() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = createConcept(browser, token, "twice");
        browser.send("DELETE", CONCEPTS + "/" + id, null, token, null);
        long before = currentVersion(jdbcTemplate);

        HttpResponse<String> second = browser.send("DELETE", CONCEPTS + "/" + id, null, token, null);

        assertProblem(second, 404, "CONCEPT_NOT_FOUND");
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    private String createConcept(
            Browser browser, String token, String key, ObjectNode... expressions) throws Exception {
        HttpResponse<String> response = browser.post(
                CONCEPTS, payload(key, "Name of " + key, null, true, expressions), token);
        assertThat(response.statusCode()).isEqualTo(201);
        return JSON.readTree(response.body()).get("id").asString();
    }

    private long rowCount(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    private long expressionsOf(String conceptId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM retrieval_concept_expression WHERE concept_id = ?::uuid",
                Long.class, conceptId);
    }
}
