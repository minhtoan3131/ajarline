package vn.io.ajarline.content.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.ContentTestData.insertArticle;
import static vn.io.ajarline.support.NewsTestData.insertCompleteDraft;
import static vn.io.ajarline.support.NewsTestData.insertNews;
import static vn.io.ajarline.support.TopicTestData.insertAssignment;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.administration.api.AbstractAdminAuthenticationIT;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class
})
class PublicNewsApiIT extends AbstractAdminAuthenticationIT {

    private static final String PUBLIC_NEWS = "/api/v1/news";
    private static final String ADMIN_CONTENT = "/api/admin/v1/content";
    private static final UUID NEWS_ID = uuid(1);
    private static final UUID JAVA = uuid(11);

    @Autowired
    private JdbcTemplate jdbc;

    private ListAppender<ILoggingEvent> handlerLog;

    @BeforeEach
    void prepare() {
        // content_topic and news rows go with their content_item; topic is restricted, so it goes last.
        jdbc.update("DELETE FROM content_item");
        jdbc.update("DELETE FROM topic");

        handlerLog = new ListAppender<>();
        handlerLog.start();
        handlerLogger().addAppender(handlerLog);
    }

    @AfterEach
    void detachLog() {
        handlerLogger().detachAppender(handlerLog);
    }

    @Test
    void get_ShouldReturnExactPublicContract_WhenNewsIsPublished() throws Exception {
        insertTopic(jdbc, JAVA, "java", "Java", true);
        insertNews(jdbc, NEWS_ID, "java-news", PublicationStatus.PUBLISHED,
                "A summary", "# A body", "Example Times", "https://example.com/story");
        jdbc.update("UPDATE news SET source_published_at = ? WHERE content_id = ?",
                OffsetDateTime.parse("2026-11-10T03:00:00Z"), NEWS_ID);
        insertAssignment(jdbc, NEWS_ID, JAVA);

        HttpResponse<String> response = new Browser().get(PUBLIC_NEWS + "/java-news");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "slug", "title", "summary", "bodyMarkdown",
                "topics", "source", "publishedAt", "updatedAt");
        assertThat(body.get("id").asString()).isEqualTo(NEWS_ID.toString());
        assertThat(body.get("type").asString()).isEqualTo("news");
        assertThat(body.get("slug").asString()).isEqualTo("java-news");
        assertThat(body.get("title").asString()).isEqualTo("News java-news");
        assertThat(body.get("summary").asString()).isEqualTo("A summary");
        assertThat(body.get("bodyMarkdown").asString()).isEqualTo("# A body");
        assertThat(body.get("topics").size()).isEqualTo(1);
        assertThat(body.get("topics").get(0).propertyNames())
                .containsExactlyInAnyOrder("id", "slug", "name");
        assertThat(body.get("topics").get(0).get("slug").asString()).isEqualTo("java");
        assertThat(body.get("publishedAt").isNull()).isFalse();
        assertThat(body.get("updatedAt").isNull()).isFalse();

        JsonNode source = body.get("source");
        assertThat(source.propertyNames()).containsExactlyInAnyOrder("name", "url", "publishedAt");
        assertThat(source.get("name").asString()).isEqualTo("Example Times");
        assertThat(source.get("url").asString()).isEqualTo("https://example.com/story");
        assertThat(source.get("publishedAt").asString()).startsWith("2026-11-10T03:00:00");

        for (String internal : new String[]{
                "status", "revision", "unpublishedAt", "createdAt", "topicIds",
                "moduleId", "position", "series", "module"}) {
            assertThat(body.has(internal)).as(internal).isFalse();
        }
    }

    @Test
    void get_ShouldKeepSourceDateKeyWithNull_WhenSourceHasNoDate() throws Exception {
        insertNews(jdbc, NEWS_ID, "no-date-news", PublicationStatus.PUBLISHED,
                "A summary", "# A body", "Example Times", "https://example.com/story");

        JsonNode source = JSON.readTree(
                new Browser().get(PUBLIC_NEWS + "/no-date-news").body()).get("source");

        assertThat(source.propertyNames()).containsExactlyInAnyOrder("name", "url", "publishedAt");
        assertThat(source.get("publishedAt").isNull()).isTrue();
    }

    @Test
    void get_ShouldUseSame404_ForEveryReasonNewsIsNotPublic() throws Exception {
        insertNews(jdbc, uuid(2), "draft-news", PublicationStatus.DRAFT,
                "A summary", "# A body", "Source", "https://example.com/a");
        insertNews(jdbc, uuid(3), "unpublished-news", PublicationStatus.UNPUBLISHED,
                "A summary", "# A body", "Source", "https://example.com/a");
        // An Article may share a slug with News, but it must never be read as News.
        insertArticle(jdbc, uuid(4), "article-slug", "An Article", PublicationStatus.PUBLISHED);
        Browser reader = new Browser();

        HttpResponse<String> missing = reader.get(PUBLIC_NEWS + "/missing-news");
        HttpResponse<String> draft = reader.get(PUBLIC_NEWS + "/draft-news");
        HttpResponse<String> unpublished = reader.get(PUBLIC_NEWS + "/unpublished-news");
        HttpResponse<String> articleSlug = reader.get(PUBLIC_NEWS + "/article-slug");
        HttpResponse<String> badSlug = reader.get(PUBLIC_NEWS + "/Not_A_Slug");

        for (HttpResponse<String> response : List.of(missing, draft, unpublished, articleSlug, badSlug)) {
            assertPublicNotFound(response);
            assertSameDisclosure(missing, response);
        }
    }

    @Test
    void lifecycle_ShouldChangePublicVisibilityImmediately() throws Exception {
        Browser admin = authenticatedBrowser();
        String token = csrf(admin);
        insertCompleteDraft(jdbc, NEWS_ID, "lifecycle-news");
        Browser reader = new Browser();

        assertPublicNotFound(reader.get(PUBLIC_NEWS + "/lifecycle-news"));

        assertThat(admin.post(ADMIN_CONTENT + "/" + NEWS_ID + "/publish",
                null, token, "\"1\"").statusCode()).isEqualTo(200);
        assertThat(reader.get(PUBLIC_NEWS + "/lifecycle-news").statusCode()).isEqualTo(200);

        jdbc.update("UPDATE content_item SET title = 'Changed title' WHERE id = ?", NEWS_ID);
        JsonNode changed = JSON.readTree(reader.get(PUBLIC_NEWS + "/lifecycle-news").body());
        assertThat(changed.get("title").asString()).isEqualTo("Changed title");

        assertThat(admin.post(ADMIN_CONTENT + "/" + NEWS_ID + "/unpublish",
                null, token, "\"2\"").statusCode()).isEqualTo(200);
        assertPublicNotFound(reader.get(PUBLIC_NEWS + "/lifecycle-news"));
    }

    @Test
    void get_ShouldReturnServerErrorAndLogOnce_WhenNewsRowIsMissing() throws Exception {
        insertNews(jdbc, NEWS_ID, "no-row-news", PublicationStatus.PUBLISHED,
                "A summary", "# A body", "Source", "https://example.com/a");
        jdbc.update("DELETE FROM news WHERE content_id = ?", NEWS_ID);

        assertServerErrorIsLogged("no-row-news");
    }

    @Test
    void get_ShouldReturnServerErrorAndLogOnce_WhenPublishedBodyIsEmpty() throws Exception {
        insertNews(jdbc, NEWS_ID, "no-body-news", PublicationStatus.PUBLISHED,
                "A summary", "# A body", "Source", "https://example.com/a");
        jdbc.update("UPDATE news SET body_markdown = NULL WHERE content_id = ?", NEWS_ID);

        assertServerErrorIsLogged("no-body-news");
    }

    private void assertServerErrorIsLogged(String slug) throws Exception {
        HttpResponse<String> response = new Browser().get(PUBLIC_NEWS + "/" + slug);

        assertProblem(response, 500, "INTERNAL_ERROR");
        String requestId = JSON.readTree(response.body()).get("requestId").asString();
        List<ILoggingEvent> errors = handlerLog.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .toList();
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().getFormattedMessage()).contains(requestId);
        assertThat(errors.getFirst().getThrowableProxy().getClassName())
                .isEqualTo("java.lang.IllegalStateException");
    }

    private static Logger handlerLogger() {
        return (Logger) LoggerFactory.getLogger(PublicNewsExceptionHandler.class);
    }

    private static void assertPublicNotFound(HttpResponse<String> response) {
        assertProblem(response, 404, "CONTENT_NOT_FOUND");
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("title").asString()).isEqualTo("News is not publicly available");
        assertThat(body.get("detail").asString()).isEqualTo("News is not publicly available.");
    }

    private static void assertSameDisclosure(
            HttpResponse<String> first, HttpResponse<String> second) {
        JsonNode left = JSON.readTree(first.body());
        JsonNode right = JSON.readTree(second.body());
        for (String field : new String[]{"type", "title", "status", "detail", "code"}) {
            assertThat(left.get(field)).isEqualTo(right.get(field));
        }
    }
}
