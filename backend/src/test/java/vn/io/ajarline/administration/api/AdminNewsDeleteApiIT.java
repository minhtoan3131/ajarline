package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.node.ObjectNode;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.ContentTestData;
import vn.io.ajarline.support.NewsTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;
import vn.io.ajarline.support.TopicTestData;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminNewsDeleteApiIT extends AbstractAdminAuthenticationIT {

    private static final String NEWS = "/api/admin/v1/news";
    private static final UUID NEWS_ID = uuid(1);
    private static final UUID OTHER_NEWS_ID = uuid(2);
    private static final UUID ARTICLE_ID = uuid(3);
    private static final UUID TOPIC_ID = uuid(4);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearBeforeTest() {
        clear();
    }

    @AfterEach
    void clearAfterTest() {
        clear();
    }

    @Test
    void delete_ShouldRemoveNewsAndItsTopicLinks_WhenDraftHasTheCurrentRevision() throws Exception {
        NewsTestData.insertCompleteDraft(jdbcTemplate, NEWS_ID, "doomed-news");
        NewsTestData.insertCompleteDraft(jdbcTemplate, OTHER_NEWS_ID, "other-news");
        TopicTestData.insertTopic(jdbcTemplate, TOPIC_ID, "a-topic", "A topic", true);
        TopicTestData.insertAssignment(jdbcTemplate, NEWS_ID, TOPIC_ID);
        TopicTestData.insertAssignment(jdbcTemplate, OTHER_NEWS_ID, TOPIC_ID);
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), NEWS_ID, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
        assertThat(response.headers().firstValue("ETag")).isEmpty();
        assertNoStore(response);
        assertThat(count("content_item", NEWS_ID)).isZero();
        assertThat(count("news", "content_id", NEWS_ID)).isZero();
        assertThat(count("content_topic", "content_id", NEWS_ID)).isZero();
        assertThat(count("content_item", OTHER_NEWS_ID)).isEqualTo(1);
        assertThat(count("content_topic", "content_id", OTHER_NEWS_ID)).isEqualTo(1);
        assertThat(count("topic", TOPIC_ID)).isEqualTo(1);
    }

    @Test
    void create_ShouldAcceptTheSlug_WhenNewsWithThatSlugWasDeleted() throws Exception {
        NewsTestData.insertCompleteDraft(jdbcTemplate, NEWS_ID, "reused-slug");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        assertThat(remove(browser, token, NEWS_ID, "\"1\"").statusCode()).isEqualTo(204);

        HttpResponse<String> created = browser.post(NEWS, payload("reused-slug"), token);

        assertThat(created.statusCode()).isEqualTo(201);
    }

    @ParameterizedTest
    @EnumSource(value = PublicationStatus.class, names = {"PUBLISHED", "UNPUBLISHED"})
    void delete_ShouldReturnConflict_WhenNewsWasPublishedOnce(PublicationStatus status) throws Exception {
        NewsTestData.insertNews(jdbcTemplate, NEWS_ID, "was-public", status,
                "Summary", "# Body", "A source", "https://example.com/story");
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), NEWS_ID, "\"1\"");

        assertProblem(response, 409, "CONTENT_ALREADY_EVER_PUBLISHED");
        assertThat(count("content_item", NEWS_ID)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnPreconditionRequired_WhenIfMatchIsMissing() throws Exception {
        NewsTestData.insertCompleteDraft(jdbcTemplate, NEWS_ID, "no-if-match");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> existing = remove(browser, token, NEWS_ID, null);
        // The header is read before the database, so an unknown id gives 428 too.
        HttpResponse<String> unknown = remove(browser, token, uuid(404), null);

        assertProblem(existing, 428, "REVISION_PRECONDITION_REQUIRED");
        assertProblem(unknown, 428, "REVISION_PRECONDITION_REQUIRED");
        assertThat(count("content_item", NEWS_ID)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "W/\"1\"", "1", "\"0\"", "\"abc\""})
    void delete_ShouldReturnBadRequest_WhenIfMatchIsNotOneStrongRevision(String ifMatch) throws Exception {
        NewsTestData.insertCompleteDraft(jdbcTemplate, NEWS_ID, "bad-if-match");
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), NEWS_ID, ifMatch);

        assertProblem(response, 400, "INVALID_REQUEST");
        assertThat(count("content_item", NEWS_ID)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnPreconditionFailed_WhenRevisionIsStale() throws Exception {
        NewsTestData.insertCompleteDraft(jdbcTemplate, NEWS_ID, "stale-revision");
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), NEWS_ID, "\"2\"");

        assertProblem(response, 412, "REVISION_CONFLICT");
        assertThat(count("content_item", NEWS_ID)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnStaleBeforeConflict_WhenNewsWasPublishedAndRevisionIsStale() throws Exception {
        NewsTestData.insertNews(jdbcTemplate, NEWS_ID, "public-and-stale", PublicationStatus.PUBLISHED,
                "Summary", "# Body", "A source", "https://example.com/story");
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), NEWS_ID, "\"9\"");

        assertProblem(response, 412, "REVISION_CONFLICT");
    }

    @Test
    void delete_ShouldReturnNotFound_WhenIdIsUnknownOrBelongsToAnArticle() throws Exception {
        ContentTestData.insertArticle(jdbcTemplate, ARTICLE_ID, "an-article", "An article",
                PublicationStatus.DRAFT);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> unknown = remove(browser, token, uuid(404), "\"1\"");
        HttpResponse<String> wrongType = remove(browser, token, ARTICLE_ID, "\"1\"");

        assertProblem(unknown, 404, "CONTENT_NOT_FOUND");
        assertProblem(wrongType, 404, "CONTENT_NOT_FOUND");
        assertThat(count("content_item", ARTICLE_ID)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnBadRequest_WhenIdIsNotAUuid() throws Exception {
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.send(
                "DELETE", NEWS + "/not-a-uuid", null, csrf(browser), null, "\"1\"");

        assertProblem(response, 400, "INVALID_UUID");
    }

    @Test
    void delete_ShouldReturnNotFound_WhenTheSameRequestRunsTwice() throws Exception {
        NewsTestData.insertCompleteDraft(jdbcTemplate, NEWS_ID, "twice");
        NewsTestData.insertCompleteDraft(jdbcTemplate, OTHER_NEWS_ID, "untouched");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> first = remove(browser, token, NEWS_ID, "\"1\"");
        HttpResponse<String> second = remove(browser, token, NEWS_ID, "\"1\"");

        assertThat(first.statusCode()).isEqualTo(204);
        assertProblem(second, 404, "CONTENT_NOT_FOUND");
        assertThat(count("content_item", OTHER_NEWS_ID)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnUnauthorized_WhenSessionIsMissing() throws Exception {
        NewsTestData.insertCompleteDraft(jdbcTemplate, NEWS_ID, "anonymous");
        Browser browser = new Browser();

        HttpResponse<String> response = remove(browser, csrf(browser), NEWS_ID, "\"1\"");

        assertProblem(response, 401, "AUTHENTICATION_REQUIRED");
        assertThat(count("content_item", NEWS_ID)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnForbidden_WhenCsrfProofIsMissingOrWrong() throws Exception {
        NewsTestData.insertCompleteDraft(jdbcTemplate, NEWS_ID, "no-csrf");
        Browser browser = authenticatedBrowser();
        csrf(browser);

        HttpResponse<String> missing = remove(browser, null, NEWS_ID, "\"1\"");
        HttpResponse<String> wrong = remove(browser, "invalid", NEWS_ID, "\"1\"");

        assertProblem(missing, 403, "CSRF_INVALID");
        assertProblem(wrong, 403, "CSRF_INVALID");
        assertThat(count("content_item", NEWS_ID)).isEqualTo(1);
    }

    private HttpResponse<String> remove(Browser browser, String token, UUID id, String ifMatch)
            throws Exception {
        return browser.send("DELETE", NEWS + "/" + id, null, token, null, ifMatch);
    }

    private ObjectNode payloadNode(String slug) {
        ObjectNode body = JSON.createObjectNode();
        body.put("title", "A News Item");
        body.put("slug", slug);
        body.put("summary", "A short summary.");
        body.put("bodyMarkdown", "# Body");
        ObjectNode source = body.putObject("source");
        source.put("name", "A source");
        source.put("url", "https://example.com/story");
        source.putNull("publishedAt");
        body.putArray("topicIds");
        return body;
    }

    private String payload(String slug) {
        return JSON.writeValueAsString(payloadNode(slug));
    }

    private long count(String table, UUID id) {
        return count(table, "id", id);
    }

    private long count(String table, String column, UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE " + column + " = ?", Long.class, id);
    }

    private void clear() {
        jdbcTemplate.update("DELETE FROM content_item");
        jdbcTemplate.update("DELETE FROM topic");
    }
}
