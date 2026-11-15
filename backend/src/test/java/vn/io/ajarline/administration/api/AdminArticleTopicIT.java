package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminArticleTopicIT extends AbstractAdminAuthenticationIT {

    private static final String ARTICLES = "/api/admin/v1/articles";
    private static final UUID JAVA = uuid(1);
    private static final UUID SPRING = uuid(2);
    private static final UUID LEGACY = uuid(3);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("DELETE FROM content_item");
        jdbcTemplate.update("DELETE FROM topic");
        insertTopic(jdbcTemplate, JAVA, "java", "Java", true);
        insertTopic(jdbcTemplate, SPRING, "spring", "Spring", true);
        insertTopic(jdbcTemplate, LEGACY, "legacy", "Legacy", false);
    }

    @Test
    void create_ShouldReturnSortedTopicIds_WhenTopicsAreAssigned() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(
                ARTICLES, editable("topic-article", "Body", SPRING, JAVA), token);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
        JsonNode body = JSON.readTree(response.body());
        assertThat(topicIds(body)).containsExactly(JAVA.toString(), SPRING.toString());
    }

    @Test
    void create_ShouldRejectRequestAndStoreNothing_WhenTopicIsInactive() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(
                ARTICLES, editable("inactive-topic", "Body", LEGACY), token);

        assertProblem(response, 422, "INACTIVE_TOPIC_ASSIGNMENT");
        assertThat(rowCount("content_item")).isZero();
    }

    @Test
    void create_ShouldRejectRequest_WhenTopicDoesNotExist() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(
                ARTICLES, editable("unknown-topic", "Body", uuid(99)), token);

        assertProblem(response, 422, "TOPIC_REFERENCE_NOT_FOUND");
        assertThat(rowCount("content_item")).isZero();
    }

    @Test
    void create_ShouldRejectRequest_WhenTopicIdsRepeat() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(
                ARTICLES, editable("repeated-topic", "Body", JAVA, JAVA), token);

        assertProblem(response, 422, "VALIDATION_FAILED");
        assertThat(rowCount("content_item")).isZero();
    }

    @Test
    void get_ShouldReturnTopicIds_WhenArticleHasTopics() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        UUID id = create(browser, token, "loaded-topics", JAVA);

        HttpResponse<String> response = browser.get(ARTICLES + "/" + id);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(topicIds(JSON.readTree(response.body()))).containsExactly(JAVA.toString());
    }

    @Test
    void update_ShouldAssignTopicsAndReturnNewEtag_WhenSetChanges() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        UUID id = create(browser, token, "assign-later");

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + id,
                editable("assign-later", "Body", JAVA, SPRING),
                token,
                "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"2\"");
        assertThat(topicIds(JSON.readTree(response.body())))
                .containsExactly(JAVA.toString(), SPRING.toString());
    }

    @Test
    void update_ShouldKeepEtagAndTime_WhenTopicSetIsUnchanged() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        UUID id = create(browser, token, "same-set", JAVA);
        JsonNode before = JSON.readTree(browser.get(ARTICLES + "/" + id).body());

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + id, editable("same-set", "Body", JAVA), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
        assertThat(JSON.readTree(response.body()).get("updatedAt")).isEqualTo(before.get("updatedAt"));
    }

    @Test
    void update_ShouldKeepAssignedTopic_WhenTopicWasTurnedOffAfterwards() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        UUID id = create(browser, token, "kept-topic", JAVA);
        jdbcTemplate.update("UPDATE topic SET active = false WHERE id = ?", JAVA);

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + id, editable("kept-topic", "Changed body", JAVA), token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(topicIds(JSON.readTree(response.body()))).containsExactly(JAVA.toString());
    }

    @Test
    void update_ShouldRejectAndKeepState_WhenInactiveTopicIsAdded() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        UUID id = create(browser, token, "rejected-add");

        HttpResponse<String> response = browser.put(
                ARTICLES + "/" + id, editable("rejected-add", "Changed body", LEGACY), token, "\"1\"");

        assertProblem(response, 422, "INACTIVE_TOPIC_ASSIGNMENT");
        HttpResponse<String> stored = browser.get(ARTICLES + "/" + id);
        assertThat(stored.headers().firstValue("ETag")).contains("\"1\"");
        JsonNode body = JSON.readTree(stored.body());
        assertThat(body.get("bodyMarkdown").asString()).isEqualTo("Body");
        assertThat(topicIds(body)).isEmpty();
    }

    private UUID create(Browser browser, String token, String slug, UUID... topicIds) throws Exception {
        HttpResponse<String> response = browser.post(
                ARTICLES, editable(slug, "Body", topicIds), token);
        assertThat(response.statusCode()).isEqualTo(201);
        return UUID.fromString(JSON.readTree(response.body()).get("id").asString());
    }

    private String editable(String slug, String bodyMarkdown, UUID... topicIds) throws Exception {
        ObjectNode body = JSON.createObjectNode();
        body.put("title", "Topic Article");
        body.put("slug", slug);
        body.put("summary", "A short summary.");
        body.put("bodyMarkdown", bodyMarkdown);
        body.putNull("moduleId");
        body.putNull("position");
        ArrayNode ids = body.putArray("topicIds");
        for (UUID topicId : topicIds) {
            ids.add(topicId.toString());
        }
        return JSON.writeValueAsString(body);
    }

    private static List<String> topicIds(JsonNode body) {
        List<String> ids = new ArrayList<>();
        body.get("topicIds").forEach(node -> ids.add(node.asString()));
        return ids;
    }

    private long rowCount(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
