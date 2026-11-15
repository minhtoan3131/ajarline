package vn.io.ajarline.content.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
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
import vn.io.ajarline.administration.api.AbstractAdminAuthenticationIT;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class
})
class PublicArticleApiIT extends AbstractAdminAuthenticationIT {

    private static final String ADMIN_ARTICLES = "/api/admin/v1/articles";
    private static final String ADMIN_CONTENT = "/api/admin/v1/content";
    private static final String PUBLIC_ARTICLES = "/api/v1/articles";

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void clearArticles() {
        // content_topic rows go with their content_item; topic is restricted, so it goes last.
        jdbc.update("DELETE FROM content_item");
        jdbc.update("DELETE FROM topic");
    }

    @Test
    void get_ShouldReturnExactPublicContract_WhenArticleIsPublished()
            throws Exception {
        Browser admin = authenticatedBrowser();
        String token = csrf(admin);
        String id = create(admin, token, "public-article");
        assertThat(admin.post(
                ADMIN_CONTENT + "/" + id + "/publish",
                null, token, "\"1\"").statusCode()).isEqualTo(200);

        Browser reader = new Browser();
        HttpResponse<String> response = reader.get(
                PUBLIC_ARTICLES + "/public-article");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "slug", "title", "summary", "bodyMarkdown",
                "topics", "series", "module", "previousArticle",
                "nextArticle", "publishedAt", "updatedAt");
        assertThat(body.get("type").asString()).isEqualTo("article");
        assertThat(body.get("slug").asString()).isEqualTo("public-article");
        assertThat(body.get("title").asString()).isEqualTo("Public Article");
        assertThat(body.get("summary").asString()).isEqualTo("Public summary");
        assertThat(body.get("bodyMarkdown").asString())
                .isEqualTo("# Public body");
        assertThat(body.get("topics").isEmpty()).isTrue();
        assertThat(body.get("series").isNull()).isTrue();
        assertThat(body.get("module").isNull()).isTrue();
        assertThat(body.get("previousArticle").isNull()).isTrue();
        assertThat(body.get("nextArticle").isNull()).isTrue();
        assertThat(body.has("status")).isFalse();
        assertThat(body.has("revision")).isFalse();
        assertThat(body.has("unpublishedAt")).isFalse();
        assertThat(body.has("createdAt")).isFalse();
        assertThat(body.get("publishedAt").isNull()).isFalse();
        assertThat(body.get("updatedAt").isNull()).isFalse();
    }

    @Test
    void get_ShouldUseSame404_ForMissingDraftAndUnpublished()
            throws Exception {
        Browser admin = authenticatedBrowser();
        String token = csrf(admin);
        String draftId = create(admin, token, "draft-public-read");
        String unpublishedId = create(
                admin, token, "unpublished-public-read");
        admin.post(
                ADMIN_CONTENT + "/" + unpublishedId + "/publish",
                null, token, "\"1\"");
        admin.post(
                ADMIN_CONTENT + "/" + unpublishedId + "/unpublish",
                null, token, "\"2\"");

        Browser reader = new Browser();
        HttpResponse<String> missing = reader.get(
                PUBLIC_ARTICLES + "/missing-article");
        HttpResponse<String> draft = reader.get(
                PUBLIC_ARTICLES + "/draft-public-read");
        HttpResponse<String> unpublished = reader.get(
                PUBLIC_ARTICLES + "/unpublished-public-read");

        assertSamePublicNotFound(missing);
        assertSamePublicNotFound(draft);
        assertSamePublicNotFound(unpublished);
        assertSameDisclosure(missing, draft);
        assertSameDisclosure(draft, unpublished);
        assertSamePublicNotFound(admin.get(
                PUBLIC_ARTICLES + "/draft-public-read"));
        assertThat(admin.get(ADMIN_ARTICLES + "/" + draftId).statusCode())
                .isEqualTo(200);
    }

    @Test
    void lifecycle_ShouldChangePublicEligibilityImmediately()
            throws Exception {
        Browser admin = authenticatedBrowser();
        String token = csrf(admin);
        String id = create(admin, token, "visibility-lifecycle");
        Browser reader = new Browser();

        assertSamePublicNotFound(reader.get(
                PUBLIC_ARTICLES + "/visibility-lifecycle"));

        admin.post(
                ADMIN_CONTENT + "/" + id + "/publish",
                null, token, "\"1\"");
        assertThat(reader.get(
                PUBLIC_ARTICLES + "/visibility-lifecycle").statusCode())
                .isEqualTo(200);

        admin.post(
                ADMIN_CONTENT + "/" + id + "/unpublish",
                null, token, "\"2\"");
        assertSamePublicNotFound(reader.get(
                PUBLIC_ARTICLES + "/visibility-lifecycle"));
    }

    @Test
    void get_ShouldNotCanonicalizeInvalidSlug() throws Exception {
        Browser admin = authenticatedBrowser();
        String token = csrf(admin);
        String id = create(admin, token, "canonical-slug");
        admin.post(
                ADMIN_CONTENT + "/" + id + "/publish",
                null, token, "\"1\"");

        assertSamePublicNotFound(new Browser().get(
                PUBLIC_ARTICLES + "/Canonical-Slug"));
    }

    @Test
    void get_ShouldListTopicsByNameThenId_WhenArticleHasTopics()
            throws Exception {
        // Two Topics share the name "Java", so the id decides their order.
        insertTopic(jdbc, uuid(1), "spring", "Spring", true);
        insertTopic(jdbc, uuid(3), "java-core", "Java", true);
        insertTopic(jdbc, uuid(2), "java", "Java", true);
        Browser admin = authenticatedBrowser();
        String token = csrf(admin);
        String id = create(admin, token, "topic-article",
                List.of(uuid(1), uuid(3), uuid(2)));
        assertThat(admin.post(
                ADMIN_CONTENT + "/" + id + "/publish",
                null, token, "\"1\"").statusCode()).isEqualTo(200);

        HttpResponse<String> response = new Browser().get(
                PUBLIC_ARTICLES + "/topic-article");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode topics = JSON.readTree(response.body()).get("topics");
        assertThat(topics.size()).isEqualTo(3);
        assertThat(topics.get(0).propertyNames())
                .containsExactlyInAnyOrder("id", "slug", "name");
        assertThat(List.of(
                topics.get(0).get("slug").asString(),
                topics.get(1).get("slug").asString(),
                topics.get(2).get("slug").asString()))
                .containsExactly("java", "java-core", "spring");
        assertThat(topics.get(0).get("id").asString())
                .isEqualTo(uuid(2).toString());
        assertThat(topics.get(0).get("name").asString()).isEqualTo("Java");
    }

    private String create(
            Browser admin,
            String token,
            String slug
    ) throws Exception {
        return create(admin, token, slug, List.of());
    }

    private String create(
            Browser admin,
            String token,
            String slug,
            List<UUID> topicIds
    ) throws Exception {
        ObjectNode request = JSON.createObjectNode();
        request.put("title", "Public Article");
        request.put("slug", slug);
        request.put("summary", "Public summary");
        request.put("bodyMarkdown", "# Public body");
        request.putNull("moduleId");
        request.putNull("position");
        ArrayNode topics = request.putArray("topicIds");
        topicIds.forEach(topicId -> topics.add(topicId.toString()));

        HttpResponse<String> response = admin.post(
                ADMIN_ARTICLES,
                JSON.writeValueAsString(request),
                token);
        assertThat(response.statusCode()).isEqualTo(201);
        return JSON.readTree(response.body()).get("id").asString();
    }

    private static void assertSamePublicNotFound(
            HttpResponse<String> response
    ) {
        assertProblem(response, 404, "CONTENT_NOT_FOUND");
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("title").asString())
                .isEqualTo("Article is not publicly available");
        assertThat(body.get("detail").asString())
                .isEqualTo("Article is not publicly available.");
    }

    private static void assertSameDisclosure(
            HttpResponse<String> first,
            HttpResponse<String> second
    ) {
        JsonNode left = JSON.readTree(first.body());
        JsonNode right = JSON.readTree(second.body());
        for (String field : new String[]{
                "type", "title", "status", "detail", "code"
        }) {
            assertThat(left.get(field)).isEqualTo(right.get(field));
        }
    }
}
