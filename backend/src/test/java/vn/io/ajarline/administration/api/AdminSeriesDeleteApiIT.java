package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
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
class AdminSeriesDeleteApiIT extends AbstractAdminAuthenticationIT {

    private static final String SERIES = "/api/admin/v1/series";
    private static final UUID SERIES_ID = uuid(1);
    private static final UUID OTHER_SERIES_ID = uuid(2);
    private static final UUID MODULE_ID = uuid(3);
    private static final UUID NEWS_ID = uuid(4);
    private static final UUID TOPIC_ID = uuid(5);

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
    void delete_ShouldRemoveSeriesAndItsTopicLink_WhenDraftHasNoModule() throws Exception {
        LearningTestData.insertSeries(jdbcTemplate, SERIES_ID, "doomed-series");
        LearningTestData.insertSeries(jdbcTemplate, OTHER_SERIES_ID, "other-series");
        TopicTestData.insertTopic(jdbcTemplate, TOPIC_ID, "a-topic", "A topic", true);
        TopicTestData.insertAssignment(jdbcTemplate, SERIES_ID, TOPIC_ID);
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), SERIES_ID, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
        assertNoStore(response);
        assertThat(count("content_item", "id", SERIES_ID)).isZero();
        assertThat(count("series", "content_id", SERIES_ID)).isZero();
        assertThat(count("content_topic", "content_id", SERIES_ID)).isZero();
        assertThat(count("topic", "id", TOPIC_ID)).isEqualTo(1);
        assertThat(count("content_item", "id", OTHER_SERIES_ID)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnInUseAndChangeNothing_WhenSeriesStillHasAModule() throws Exception {
        LearningTestData.insertSeries(jdbcTemplate, SERIES_ID, "has-module");
        LearningTestData.insertModule(jdbcTemplate, MODULE_ID, "its-module", SERIES_ID, 1);
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), SERIES_ID, "\"1\"");

        assertProblem(response, 409, "CONTENT_IN_USE");
        assertThat(count("content_item", "id", SERIES_ID)).isEqualTo(1);
        assertThat(count("module", "content_id", MODULE_ID)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnEverPublishedBeforeInUse_WhenSeriesWasPublishedAndHasAModule() throws Exception {
        LearningTestData.insertSeries(
                jdbcTemplate, SERIES_ID, "public-with-module", PublicationStatus.PUBLISHED, "# Overview");
        LearningTestData.insertModule(jdbcTemplate, MODULE_ID, "its-module", SERIES_ID, 1);
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), SERIES_ID, "\"1\"");

        assertProblem(response, 409, "CONTENT_ALREADY_EVER_PUBLISHED");
    }

    @Test
    void delete_ShouldReturnEverPublished_WhenSeriesIsUnpublished() throws Exception {
        LearningTestData.insertSeries(
                jdbcTemplate, SERIES_ID, "was-public", PublicationStatus.UNPUBLISHED, "# Overview");
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), SERIES_ID, "\"1\"");

        assertProblem(response, 409, "CONTENT_ALREADY_EVER_PUBLISHED");
        assertThat(count("content_item", "id", SERIES_ID)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnPreconditionErrorsBeforeConflicts_WhenHeaderIsMissingOrStale() throws Exception {
        LearningTestData.insertSeries(jdbcTemplate, SERIES_ID, "has-module");
        LearningTestData.insertModule(jdbcTemplate, MODULE_ID, "its-module", SERIES_ID, 1);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> missing = remove(browser, token, SERIES_ID, null);
        // The Series still has a Module, but a stale revision is reported first.
        HttpResponse<String> stale = remove(browser, token, SERIES_ID, "\"9\"");

        assertProblem(missing, 428, "REVISION_PRECONDITION_REQUIRED");
        assertProblem(stale, 412, "REVISION_CONFLICT");
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
    void delete_ShouldReturnNotFound_WhenTheSameRequestRunsTwice() throws Exception {
        LearningTestData.insertSeries(jdbcTemplate, SERIES_ID, "twice");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> first = remove(browser, token, SERIES_ID, "\"1\"");
        HttpResponse<String> second = remove(browser, token, SERIES_ID, "\"1\"");

        assertThat(first.statusCode()).isEqualTo(204);
        assertProblem(second, 404, "CONTENT_NOT_FOUND");
    }

    @Test
    void delete_ShouldReturnUnauthorized_WhenSessionIsMissing() throws Exception {
        LearningTestData.insertSeries(jdbcTemplate, SERIES_ID, "anonymous");
        Browser browser = new Browser();

        HttpResponse<String> response = remove(browser, csrf(browser), SERIES_ID, "\"1\"");

        assertProblem(response, 401, "AUTHENTICATION_REQUIRED");
        assertThat(count("content_item", "id", SERIES_ID)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnForbidden_WhenCsrfProofIsMissing() throws Exception {
        LearningTestData.insertSeries(jdbcTemplate, SERIES_ID, "no-csrf");
        Browser browser = authenticatedBrowser();
        csrf(browser);

        HttpResponse<String> response = remove(browser, null, SERIES_ID, "\"1\"");

        assertProblem(response, 403, "CSRF_INVALID");
        assertThat(count("content_item", "id", SERIES_ID)).isEqualTo(1);
    }

    private HttpResponse<String> remove(Browser browser, String token, UUID id, String ifMatch)
            throws Exception {
        return browser.send("DELETE", SERIES + "/" + id, null, token, null, ifMatch);
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
