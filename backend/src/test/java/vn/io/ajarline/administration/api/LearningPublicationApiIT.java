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
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.ContentTestData;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class LearningPublicationApiIT extends AbstractAdminAuthenticationIT {

    private static final String CONTENT = "/api/admin/v1/content";

    private static final UUID SERIES = uuid(1);
    private static final UUID MODULE = uuid(2);
    private static final UUID ARTICLE = uuid(3);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearRows() {
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @AfterEach
    void removeRows() {
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @Test
    void publishModule_ShouldPublishOnlyTheModule_WhenOneArticleIsPublished() throws Exception {
        insertLine(PublicationStatus.DRAFT, PublicationStatus.DRAFT, PublicationStatus.PUBLISHED);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(publishPath(MODULE), null, token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"2\"");
        assertNoStore(response);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("id").asString()).isEqualTo(MODULE.toString());
        assertThat(body.get("type").asString()).isEqualTo("module");
        assertThat(body.get("status").asString()).isEqualTo("PUBLISHED");
        assertThat(body.get("revision").asLong()).isEqualTo(2);
        // Publishing a parent never publishes or changes another level.
        assertThat(statusOf(SERIES)).isEqualTo("DRAFT");
        assertThat(revisionOf(SERIES)).isEqualTo(1);
        assertThat(revisionOf(ARTICLE)).isEqualTo(1);
    }

    @Test
    void publishModule_ShouldReturnUnprocessable_WhenModuleHasNoPublicArticle() throws Exception {
        insertLine(PublicationStatus.DRAFT, PublicationStatus.DRAFT, PublicationStatus.DRAFT);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(publishPath(MODULE), null, token, "\"1\"");

        assertProblem(response, 422, "MODULE_HAS_NO_PUBLIC_ARTICLE");
        assertThat(statusOf(MODULE)).isEqualTo("DRAFT");
        assertThat(revisionOf(MODULE)).isEqualTo(1);
    }

    @Test
    void publishModule_ShouldReturnUnprocessable_WhenOverviewIsBlank() throws Exception {
        insertLine(PublicationStatus.DRAFT, PublicationStatus.DRAFT, PublicationStatus.PUBLISHED);
        jdbcTemplate.update("UPDATE module SET overview_markdown = '' WHERE content_id = ?", MODULE);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(publishPath(MODULE), null, token, "\"1\"");

        assertProblem(response, 422, "MODULE_OVERVIEW_REQUIRED");
    }

    @Test
    void publishSeries_ShouldPublishOnlyTheSeries_WhenOneModuleIsPublic() throws Exception {
        insertLine(PublicationStatus.DRAFT, PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(publishPath(SERIES), null, token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("type").asString()).isEqualTo("series");
        assertThat(body.get("status").asString()).isEqualTo("PUBLISHED");
        assertThat(body.get("revision").asLong()).isEqualTo(2);
        assertThat(revisionOf(MODULE)).isEqualTo(1);
        assertThat(revisionOf(ARTICLE)).isEqualTo(1);
    }

    @Test
    void publishSeries_ShouldReturnUnprocessable_WhenSeriesHasNoModule() throws Exception {
        LearningTestData.insertSeries(jdbcTemplate, SERIES, "a-series");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(publishPath(SERIES), null, token, "\"1\"");

        assertProblem(response, 422, "SERIES_HAS_NO_PUBLIC_MODULE");
    }

    @Test
    void publishSeries_ShouldReturnUnprocessable_WhenPublishedModuleHasNoPublicArticle() throws Exception {
        insertLine(PublicationStatus.DRAFT, PublicationStatus.PUBLISHED, PublicationStatus.DRAFT);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(publishPath(SERIES), null, token, "\"1\"");

        assertProblem(response, 422, "SERIES_HAS_NO_PUBLIC_MODULE");
        assertThat(statusOf(SERIES)).isEqualTo("DRAFT");
    }

    @Test
    void publishSeries_ShouldReturnUnprocessable_WhenModuleWithPublicArticleIsNotPublished() throws Exception {
        insertLine(PublicationStatus.DRAFT, PublicationStatus.DRAFT, PublicationStatus.PUBLISHED);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(publishPath(SERIES), null, token, "\"1\"");

        assertProblem(response, 422, "SERIES_HAS_NO_PUBLIC_MODULE");
    }

    @Test
    void publishSeries_ShouldReportFieldsBeforeChildren_WhenSummaryIsMissing() throws Exception {
        // The Series has no Module either, but the summary is reported first.
        LearningTestData.insertSeries(jdbcTemplate, SERIES, "a-series");
        jdbcTemplate.update("UPDATE content_item SET summary = NULL WHERE id = ?", SERIES);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(publishPath(SERIES), null, token, "\"1\"");

        assertProblem(response, 422, "CONTENT_SUMMARY_REQUIRED");
    }

    @Test
    void publishSeries_ShouldReturnUnprocessable_WhenOverviewIsBlank() throws Exception {
        insertLine(PublicationStatus.DRAFT, PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED);
        jdbcTemplate.update("UPDATE series SET overview_markdown = '   ' WHERE content_id = ?", SERIES);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(publishPath(SERIES), null, token, "\"1\"");

        assertProblem(response, 422, "SERIES_OVERVIEW_REQUIRED");
    }

    @Test
    void publishSeries_ShouldChangeNothing_WhenSeriesIsAlreadyPublished() throws Exception {
        insertLine(PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(publishPath(SERIES), null, token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("ETag")).contains("\"1\"");
        assertThat(revisionOf(SERIES)).isEqualTo(1);
    }

    @Test
    void unpublishSeries_ShouldLeaveModuleAndArticleUntouched_WhenSeriesIsPublished() throws Exception {
        insertLine(PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(unpublishPath(SERIES), null, token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("status").asString()).isEqualTo("UNPUBLISHED");
        assertThat(body.get("revision").asLong()).isEqualTo(2);
        assertThat(statusOf(MODULE)).isEqualTo("PUBLISHED");
        assertThat(revisionOf(MODULE)).isEqualTo(1);
        assertThat(statusOf(ARTICLE)).isEqualTo("PUBLISHED");
        assertThat(revisionOf(ARTICLE)).isEqualTo(1);
    }

    @Test
    void unpublishModule_ShouldLeaveSeriesAndArticleUntouched_WhenModuleIsPublished() throws Exception {
        insertLine(PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(unpublishPath(MODULE), null, token, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(statusOf(SERIES)).isEqualTo("PUBLISHED");
        assertThat(revisionOf(SERIES)).isEqualTo(1);
        assertThat(statusOf(ARTICLE)).isEqualTo("PUBLISHED");
        assertThat(revisionOf(ARTICLE)).isEqualTo(1);
    }

    @Test
    void unpublishSeries_ShouldReturnConflict_WhenSeriesIsDraft() throws Exception {
        insertLine(PublicationStatus.DRAFT, PublicationStatus.DRAFT, PublicationStatus.DRAFT);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(unpublishPath(SERIES), null, token, "\"1\"");

        assertProblem(response, 409, "INVALID_PUBLICATION_STATE");
        assertThat(revisionOf(SERIES)).isEqualTo(1);
    }

    @Test
    void publishSeries_ShouldReturnPreconditionFailed_WhenRevisionIsStale() throws Exception {
        insertLine(PublicationStatus.DRAFT, PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHED);
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> response = browser.post(publishPath(SERIES), null, token, "\"5\"");

        assertProblem(response, 412, "REVISION_CONFLICT");
        assertThat(statusOf(SERIES)).isEqualTo("DRAFT");
    }

    /**
     * Inserts one Series with one Module and one Article, each in the given status.
     * The Article is in the Module and the Module is in the Series.
     */
    private void insertLine(PublicationStatus series, PublicationStatus module, PublicationStatus article) {
        LearningTestData.insertSeries(jdbcTemplate, SERIES, "a-series", series, "Fixture overview");
        LearningTestData.insertModule(jdbcTemplate, MODULE, "a-module", SERIES, 1, module, "Fixture overview");
        ContentTestData.insertArticle(jdbcTemplate, ARTICLE, "an-article", "An article", article);
        LearningTestData.insertModuleArticle(jdbcTemplate, ARTICLE, MODULE, 1);
    }

    private static String publishPath(UUID id) {
        return CONTENT + "/" + id + "/publish";
    }

    private static String unpublishPath(UUID id) {
        return CONTENT + "/" + id + "/unpublish";
    }

    private String statusOf(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM content_item WHERE id = ?", String.class, id);
    }

    private long revisionOf(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT revision FROM content_item WHERE id = ?", Long.class, id);
    }
}
