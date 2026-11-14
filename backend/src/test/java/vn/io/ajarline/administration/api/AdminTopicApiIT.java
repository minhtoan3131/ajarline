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
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminTopicApiIT extends AbstractAdminAuthenticationIT {

    private static final String TOPICS = "/api/admin/v1/topics";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTopics() {
        jdbcTemplate.update("DELETE FROM content_item");
        jdbcTemplate.update("DELETE FROM topic");
    }

    @Test
    void create_ShouldReturnTopic_WhenPayloadIsValid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(
                TOPICS, payload("java-basics", "Java basics", "Core Java.", true), token);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        assertThat(response.headers().firstValue("ETag")).isEmpty();
        assertNoStore(response);
        JsonNode body = JSON.readTree(response.body());
        assertItem(body, "java-basics", "Java basics", "Core Java.", true);
        assertThat(response.headers().firstValue("Location"))
                .contains(TOPICS + "/" + body.get("id").asString());
        assertThat(rowCount()).isEqualTo(1);
    }

    @Test
    void create_ShouldNormalizeSlugAndStoreNullDescription_WhenValuesNeedNormalizing() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(
                TOPICS, payload("  Java-Basics ", "Java basics", "", true), token);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("slug").asString()).isEqualTo("java-basics");
        assertThat(body.get("description").isNull()).isTrue();
    }

    @Test
    void create_ShouldRejectTopic_WhenSlugAlreadyExists() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        browser.post(TOPICS, payload("same-slug", "First", null, true), token);

        HttpResponse<String> response = browser.post(
                TOPICS, payload("same-slug", "Second", null, true), token);

        assertProblem(response, 409, "SLUG_CONFLICT");
        assertThat(rowCount()).isEqualTo(1);
    }

    @Test
    void create_ShouldRejectTopic_WhenSlugFormatIsInvalid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(
                TOPICS, payload("Not a slug", "Name", null, true), token);

        assertProblem(response, 422, "VALIDATION_FAILED");
        assertThat(rowCount()).isZero();
    }

    @Test
    void list_ShouldReturnEmptyArray_WhenNoTopicExists() throws Exception {
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.get(TOPICS);

        assertThat(response.statusCode()).isEqualTo(200);
        assertNoStore(response);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames()).containsExactly("topics");
        assertThat(body.get("topics").isArray()).isTrue();
        assertThat(body.get("topics")).isEmpty();
    }

    @Test
    void list_ShouldReturnActiveAndInactiveTopicsOrderedByName_WhenTopicsExist() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        browser.post(TOPICS, payload("gamma", "Gamma", null, true), token);
        browser.post(TOPICS, payload("alpha", "Alpha", "Inactive one.", false), token);
        browser.post(TOPICS, payload("beta", "Beta", null, true), token);

        HttpResponse<String> response = browser.get(TOPICS);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode topics = JSON.readTree(response.body()).get("topics");
        assertThat(topics).hasSize(3);
        assertItem(topics.get(0), "alpha", "Alpha", "Inactive one.", false);
        assertItem(topics.get(1), "beta", "Beta", null, true);
        assertItem(topics.get(2), "gamma", "Gamma", null, true);
    }

    private String payload(String slug, String name, String description, boolean active)
            throws Exception {
        ObjectNode body = JSON.createObjectNode();
        body.put("slug", slug);
        body.put("name", name);
        if (description == null) {
            body.putNull("description");
        } else {
            body.put("description", description);
        }
        body.put("active", active);
        return JSON.writeValueAsString(body);
    }

    private static void assertItem(
            JsonNode body, String slug, String name, String description, boolean active) {
        assertThat(body.propertyNames()).containsExactlyInAnyOrder(
                "id", "slug", "name", "description", "active", "createdAt", "updatedAt");
        assertThat(body.get("id").asString()).isNotBlank();
        assertThat(body.get("slug").asString()).isEqualTo(slug);
        assertThat(body.get("name").asString()).isEqualTo(name);
        if (description == null) {
            assertThat(body.get("description").isNull()).isTrue();
        } else {
            assertThat(body.get("description").asString()).isEqualTo(description);
        }
        assertThat(body.get("active").asBoolean()).isEqualTo(active);
        assertThat(body.get("createdAt").asString()).isNotBlank();
        assertThat(body.get("updatedAt").asString()).isNotBlank();
    }

    @Test
    void update_ShouldReturnUpdatedTopic_WhenPayloadIsValid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        JsonNode created = JSON.readTree(browser.post(
                TOPICS, payload("java-basics", "Java", null, true), token).body());
        String id = created.get("id").asString();

        HttpResponse<String> response = browser.put(
                TOPICS + "/" + id, editPayload("Java basics", "Core Java.", false), token, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertNoStore(response);
        assertThat(response.headers().firstValue("ETag")).isEmpty();
        JsonNode body = JSON.readTree(response.body());
        assertItem(body, "java-basics", "Java basics", "Core Java.", false);
        assertThat(body.get("id").asString()).isEqualTo(id);
    }

    @Test
    void update_ShouldRejectRequest_WhenPayloadContainsSlug() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = JSON.readTree(browser.post(
                TOPICS, payload("java-basics", "Java", null, true), token).body())
                .get("id").asString();

        HttpResponse<String> response = browser.put(
                TOPICS + "/" + id, payload("other-slug", "Java", null, true), token, null);

        assertProblem(response, 400, "INVALID_REQUEST");
        HttpResponse<String> list = browser.get(TOPICS);
        assertThat(JSON.readTree(list.body()).get("topics").get(0).get("slug").asString())
                .isEqualTo("java-basics");
    }

    @Test
    void update_ShouldRejectRequest_WhenTopicDoesNotExist() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.put(
                TOPICS + "/00000000-0000-0000-0000-000000000404",
                editPayload("Name", null, true), token, null);

        assertProblem(response, 404, "TOPIC_NOT_FOUND");
    }

    @Test
    void update_ShouldRejectRequest_WhenValuesAreInvalid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String id = JSON.readTree(browser.post(
                TOPICS, payload("java-basics", "Java", null, true), token).body())
                .get("id").asString();

        HttpResponse<String> response = browser.put(
                TOPICS + "/" + id, editPayload("   ", null, true), token, null);

        assertProblem(response, 422, "VALIDATION_FAILED");
    }

    private String editPayload(String name, String description, boolean active) throws Exception {
        ObjectNode body = JSON.createObjectNode();
        body.put("name", name);
        if (description == null) {
            body.putNull("description");
        } else {
            body.put("description", description);
        }
        body.put("active", active);
        return JSON.writeValueAsString(body);
    }

    private long rowCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM topic", Long.class);
    }
}
