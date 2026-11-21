package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.ContentTestData;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.NewsTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;
import vn.io.ajarline.support.TopicTestData;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminArticleDeleteApiIT extends AbstractAdminAuthenticationIT {

    private static final String ARTICLES = "/api/admin/v1/articles";
    private static final UUID SERIES = uuid(1);
    private static final UUID MODULE = uuid(2);
    private static final UUID OTHER_MODULE = uuid(3);
    private static final UUID FIRST = uuid(11);
    private static final UUID SECOND = uuid(12);
    private static final UUID THIRD = uuid(13);
    private static final UUID FOURTH = uuid(14);
    private static final UUID OTHER_ARTICLE = uuid(15);
    private static final UUID STANDALONE = uuid(20);
    private static final UUID NEWS_ID = uuid(21);
    private static final UUID TOPIC_ID = uuid(30);

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
    void delete_ShouldRemoveArticleAndItsTopicLink_WhenArticleIsStandalone() throws Exception {
        ContentTestData.insertArticle(jdbcTemplate, STANDALONE, "alone", "Alone", PublicationStatus.DRAFT);
        TopicTestData.insertTopic(jdbcTemplate, TOPIC_ID, "a-topic", "A topic", true);
        TopicTestData.insertAssignment(jdbcTemplate, STANDALONE, TOPIC_ID);
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), STANDALONE, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
        assertNoStore(response);
        assertThat(count("content_item", "id", STANDALONE)).isZero();
        assertThat(count("article", "content_id", STANDALONE)).isZero();
        assertThat(count("content_topic", "content_id", STANDALONE)).isZero();
        assertThat(count("topic", "id", TOPIC_ID)).isEqualTo(1);
    }

    @Test
    void delete_ShouldCloseTheGapAndRaiseRevisions_WhenArticleIsInTheMiddleOfAModule() throws Exception {
        insertModuleWithFourArticles();
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), SECOND, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(count("module_article", "article_id", SECOND)).isZero();
        assertThat(articlesInOrder(MODULE)).containsExactly(FIRST, THIRD, FOURTH);
        assertThat(positions(MODULE)).containsExactly(1, 2, 3);
        assertThat(revisionOf(MODULE)).isEqualTo(2);
        assertThat(revisionOf(THIRD)).isEqualTo(2);
        assertThat(revisionOf(FOURTH)).isEqualTo(2);
        assertThat(revisionOf(FIRST)).isEqualTo(1);
        assertThat(revisionOf(SERIES)).isEqualTo(1);
        assertThat(revisionOf(OTHER_MODULE)).isEqualTo(1);
        assertThat(revisionOf(OTHER_ARTICLE)).isEqualTo(1);
    }

    @Test
    void delete_ShouldRaiseOnlyTheModule_WhenArticleIsTheLastOne() throws Exception {
        insertModuleWithFourArticles();
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), FOURTH, "\"1\"");

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(positions(MODULE)).containsExactly(1, 2, 3);
        assertThat(revisionOf(MODULE)).isEqualTo(2);
        assertThat(revisionOf(FIRST)).isEqualTo(1);
        assertThat(revisionOf(THIRD)).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = PublicationStatus.class, names = {"PUBLISHED", "UNPUBLISHED"})
    void delete_ShouldReturnConflictAndChangeNothing_WhenArticleWasPublishedOnce(PublicationStatus status)
            throws Exception {
        insertModuleWithFourArticles();
        jdbcTemplate.update(
                "UPDATE content_item SET status = ?, published_at = now() WHERE id = ?",
                status.name(), SECOND);
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = remove(browser, csrf(browser), SECOND, "\"1\"");

        assertProblem(response, 409, "CONTENT_ALREADY_EVER_PUBLISHED");
        assertThat(positions(MODULE)).containsExactly(1, 2, 3, 4);
        assertThat(revisionOf(MODULE)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnPreconditionErrors_WhenIfMatchIsMissingOrStale() throws Exception {
        insertModuleWithFourArticles();
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> missing = remove(browser, token, SECOND, null);
        HttpResponse<String> stale = remove(browser, token, SECOND, "\"5\"");

        assertProblem(missing, 428, "REVISION_PRECONDITION_REQUIRED");
        assertProblem(stale, 412, "REVISION_CONFLICT");
        assertThat(positions(MODULE)).containsExactly(1, 2, 3, 4);
        assertThat(revisionOf(MODULE)).isEqualTo(1);
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
        insertModuleWithFourArticles();
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> first = remove(browser, token, SECOND, "\"1\"");
        HttpResponse<String> second = remove(browser, token, SECOND, "\"1\"");

        assertThat(first.statusCode()).isEqualTo(204);
        assertProblem(second, 404, "CONTENT_NOT_FOUND");
        assertThat(positions(MODULE)).containsExactly(1, 2, 3);
        assertThat(revisionOf(MODULE)).isEqualTo(2);
    }

    @Test
    void delete_ShouldReturnUnauthorized_WhenSessionIsMissing() throws Exception {
        ContentTestData.insertArticle(jdbcTemplate, STANDALONE, "anonymous", "Anonymous",
                PublicationStatus.DRAFT);
        Browser browser = new Browser();

        HttpResponse<String> response = remove(browser, csrf(browser), STANDALONE, "\"1\"");

        assertProblem(response, 401, "AUTHENTICATION_REQUIRED");
        assertThat(count("content_item", "id", STANDALONE)).isEqualTo(1);
    }

    @Test
    void delete_ShouldReturnForbidden_WhenCsrfProofIsMissing() throws Exception {
        ContentTestData.insertArticle(jdbcTemplate, STANDALONE, "no-csrf", "No CSRF",
                PublicationStatus.DRAFT);
        Browser browser = authenticatedBrowser();
        csrf(browser);

        HttpResponse<String> response = remove(browser, null, STANDALONE, "\"1\"");

        assertProblem(response, 403, "CSRF_INVALID");
        assertThat(count("content_item", "id", STANDALONE)).isEqualTo(1);
    }

    private void insertModuleWithFourArticles() {
        LearningTestData.insertSeries(jdbcTemplate, SERIES, "a-series");
        LearningTestData.insertModule(jdbcTemplate, MODULE, "a-module", SERIES, 1);
        LearningTestData.insertModule(jdbcTemplate, OTHER_MODULE, "other-module", SERIES, 2);
        LearningTestData.insertArticleIn(jdbcTemplate, FIRST, "first", PublicationStatus.DRAFT, MODULE, 1);
        LearningTestData.insertArticleIn(jdbcTemplate, SECOND, "second", PublicationStatus.DRAFT, MODULE, 2);
        LearningTestData.insertArticleIn(jdbcTemplate, THIRD, "third", PublicationStatus.DRAFT, MODULE, 3);
        LearningTestData.insertArticleIn(jdbcTemplate, FOURTH, "fourth", PublicationStatus.DRAFT, MODULE, 4);
        LearningTestData.insertArticleIn(
                jdbcTemplate, OTHER_ARTICLE, "other", PublicationStatus.DRAFT, OTHER_MODULE, 1);
    }

    private HttpResponse<String> remove(Browser browser, String token, UUID id, String ifMatch)
            throws Exception {
        return browser.send("DELETE", ARTICLES + "/" + id, null, token, null, ifMatch);
    }

    private long revisionOf(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT revision FROM content_item WHERE id = ?", Long.class, id);
    }

    private List<UUID> articlesInOrder(UUID moduleId) {
        return jdbcTemplate.queryForList(
                "SELECT article_id FROM module_article WHERE module_id = ? ORDER BY position",
                UUID.class, moduleId);
    }

    private List<Integer> positions(UUID moduleId) {
        return jdbcTemplate.queryForList(
                "SELECT position FROM module_article WHERE module_id = ? ORDER BY position",
                Integer.class, moduleId);
    }

    private long count(String table, String column, UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE " + column + " = ?", Long.class, id);
    }

    private void clear() {
        // Hierarchy foreign keys are RESTRICT, so children go first.
        LearningTestData.deleteAll(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM topic");
    }
}
