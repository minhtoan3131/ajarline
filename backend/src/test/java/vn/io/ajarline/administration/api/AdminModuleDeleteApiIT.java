package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.NewsTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;
import vn.io.ajarline.support.TopicTestData;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminModuleDeleteApiIT extends AbstractAdminAuthenticationIT {

    private static final String MODULES = "/api/admin/v1/modules";
    private static final UUID SERIES = uuid(1);
    private static final UUID OTHER_SERIES = uuid(2);
    private static final UUID FIRST = uuid(11);
    private static final UUID SECOND = uuid(12);
    private static final UUID THIRD = uuid(13);
    private static final UUID OTHER_MODULE = uuid(14);
    private static final UUID ARTICLE = uuid(21);
    private static final UUID NEWS_ID = uuid(22);
    private static final UUID TOPIC_ID = uuid(30);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void insertSeries() {
        clear();
        LearningTestData.insertSeries(jdbcTemplate, SERIES, "a-series");
        LearningTestData.insertSeries(jdbcTemplate, OTHER_SERIES, "other-series");
        LearningTestData.insertModule(jdbcTemplate, FIRST, "first", SERIES, 1);
        LearningTestData.insertModule(jdbcTemplate, SECOND, "second", SERIES, 2);
        LearningTestData.insertModule(jdbcTemplate, THIRD, "third", SERIES, 3);
        LearningTestData.insertModule(jdbcTemplate, OTHER_MODULE, "other", OTHER_SERIES, 1);
    }

    @AfterEach
    void clearAfterTest() {
        clear();
    }

    @Test
    void delete_ShouldCloseTheGapAndRaiseRevisions_WhenModuleIsInTheMiddleOfASeries() throws Exception {
        TopicTestData.insertTopic(jdbcTemplate, TOPIC_ID, "a-topic", "A topic", true);
        TopicTestData.insertAssignment(jdbcTemplate, SECOND, TOPIC_ID);
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), SECOND, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
        assertNoStore(response);
        assertThat(count("content_item", "id", SECOND)).isZero();
        assertThat(count("module", "content_id", SECOND)).isZero();
        assertThat(count("content_topic", "content_id", SECOND)).isZero();
        assertThat(count("topic", "id", TOPIC_ID)).isEqualTo(1);
        assertThat(modulesInOrder(SERIES)).containsExactly(FIRST, THIRD);
        assertThat(positions(SERIES)).containsExactly(1, 2);
        assertThat(revisionOf(SERIES)).isEqualTo(2);
        assertThat(revisionOf(THIRD)).isEqualTo(2);
        assertThat(revisionOf(FIRST)).isEqualTo(1);
        assertThat(revisionOf(OTHER_SERIES)).isEqualTo(1);
        assertThat(revisionOf(OTHER_MODULE)).isEqualTo(1);
    }

    @Test
    void delete_ShouldRaiseOnlyTheSeries_WhenModuleIsTheLastOne() throws Exception {
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), THIRD, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(positions(SERIES)).containsExactly(1, 2);
        assertThat(revisionOf(SERIES)).isEqualTo(2);
        assertThat(revisionOf(FIRST)).isEqualTo(1);
        assertThat(revisionOf(SECOND)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnInUseAndChangeNothing_WhenModuleStillHasAnArticle() throws Exception {
        LearningTestData.insertArticleIn(
                jdbcTemplate, ARTICLE, "an-article", PublicationStatus.DRAFT, FIRST, 1);
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), FIRST, "\"1\"");

        assertProblem(response, 409, "CONTENT_IN_USE");
        assertThat(count("module", "content_id", FIRST)).isEqualTo(1);
        assertThat(count("module_article", "article_id", ARTICLE)).isEqualTo(1);
        assertThat(positions(SERIES)).containsExactly(1, 2, 3);
        assertThat(revisionOf(SERIES)).isEqualTo(1);
        assertThat(revisionOf(SECOND)).isEqualTo(1);
        assertThat(revisionOf(THIRD)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnEverPublishedBeforeInUse_WhenModuleWasPublishedAndHasAnArticle() throws Exception {
        jdbcTemplate.update(
                "UPDATE content_item SET status = 'PUBLISHED', published_at = now() WHERE id = ?", FIRST);
        LearningTestData.insertArticleIn(
                jdbcTemplate, ARTICLE, "an-article", PublicationStatus.DRAFT, FIRST, 1);
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), FIRST, "\"1\"");

        assertProblem(response, 409, "CONTENT_ALREADY_EVER_PUBLISHED");
    }

    @Test
    void delete_ShouldReturnPreconditionErrors_WhenIfMatchIsMissingOrStale() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> missing = remove(browser, token, SECOND, null);
        HttpResponse<String> stale = remove(browser, token, SECOND, "\"9\"");

        assertProblem(missing, 428, "REVISION_PRECONDITION_REQUIRED");
        assertProblem(stale, 412, "REVISION_CONFLICT");
        assertThat(positions(SERIES)).containsExactly(1, 2, 3);
        assertThat(revisionOf(SERIES)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnNotFound_WhenIdIsUnknownOrBelongsToNews() throws Exception {
        NewsTestData.insertCompleteDraft(jdbcTemplate, NEWS_ID, "a-news");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> unknown = remove(browser, token, uuid(404), "\"1\"");
        HttpResponse<String> wrongType = remove(browser, token, NEWS_ID, "\"1\"");

        assertProblem(unknown, 404, "CONTENT_NOT_FOUND");
        assertProblem(wrongType, 404, "CONTENT_NOT_FOUND");
        assertThat(count("content_item", "id", NEWS_ID)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnNotFoundAndChangeNothing_WhenTheSameRequestRunsTwice() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> first = remove(browser, token, SECOND, "\"1\"");
        HttpResponse<String> second = remove(browser, token, SECOND, "\"1\"");

        assertThat(first.statusCode()).isEqualTo(204);
        assertProblem(second, 404, "CONTENT_NOT_FOUND");
        assertThat(positions(SERIES)).containsExactly(1, 2);
        assertThat(revisionOf(SERIES)).isEqualTo(2);
    }

    @Test
    void delete_ShouldReturnUnauthorized_WhenSessionIsMissing() throws Exception {
        Browser browser = new Browser();

        HttpResponse<String> response = remove(browser, csrf(browser), SECOND, "\"1\"");

        assertProblem(response, 401, "AUTHENTICATION_REQUIRED");
        assertThat(count("content_item", "id", SECOND)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnForbidden_WhenCsrfProofIsMissing() throws Exception {
        Browser browser = authenticatedBrowser();
        csrf(browser);

        HttpResponse<String> response = remove(browser, null, SECOND, "\"1\"");

        assertProblem(response, 403, "CSRF_INVALID");
        assertThat(count("content_item", "id", SECOND)).isEqualTo(1);
    }

    private HttpResponse<String> remove(Browser browser, String token, UUID id, String ifMatch)
            throws Exception {
        return browser.send("DELETE", MODULES + "/" + id, null, token, null, ifMatch);
    }

    private long revisionOf(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT revision FROM content_item WHERE id = ?", Long.class, id);
    }

    private List<UUID> modulesInOrder(UUID seriesId) {
        return jdbcTemplate.queryForList(
                "SELECT content_id FROM module WHERE series_id = ? ORDER BY position",
                UUID.class, seriesId);
    }

    private List<Integer> positions(UUID seriesId) {
        return jdbcTemplate.queryForList(
                "SELECT position FROM module WHERE series_id = ? ORDER BY position",
                Integer.class, seriesId);
    }

    private long count(String table, String column, UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE " + column + " = ?", Long.class, id);
    }

    private void clear() {
        LearningTestData.deleteAll(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM topic");
    }
}
