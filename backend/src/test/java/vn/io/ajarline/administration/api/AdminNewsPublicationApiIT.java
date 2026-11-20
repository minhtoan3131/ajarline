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
import vn.io.ajarline.support.NewsTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminNewsPublicationApiIT extends AbstractAdminAuthenticationIT {

    private static final String CONTENT = "/api/admin/v1/content";
    private static final UUID NEWS_ID = uuid(1);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearContentBeforeTest() {
        jdbcTemplate.update("DELETE FROM content_item");
    }

    @AfterEach
    void clearContentAfterTest() {
        jdbcTemplate.update("DELETE FROM content_item");
    }

    @Test
    void publish_ShouldReturnSourceRequired_WhenNewsHasNoSource() throws Exception {
        NewsTestData.insertNews(jdbcTemplate, NEWS_ID, "no-source", PublicationStatus.DRAFT,
                "Summary", "# Body", null, null);
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.post(
                CONTENT + "/" + NEWS_ID + "/publish", null, csrf(browser), "\"1\"");

        assertProblem(response, 422, "NEWS_SOURCE_REQUIRED");
    }

    @Test
    void publish_ShouldReturnBodyRequired_WhenNewsHasNoBody() throws Exception {
        NewsTestData.insertNews(jdbcTemplate, NEWS_ID, "no-body", PublicationStatus.DRAFT,
                "Summary", null, "A source", "https://example.com/story");
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.post(
                CONTENT + "/" + NEWS_ID + "/publish", null, csrf(browser), "\"1\"");

        assertProblem(response, 422, "NEWS_BODY_REQUIRED");
    }

    @Test
    void lifecycle_ShouldReturnNewsStateAndEtag_WhenNewsIsComplete() throws Exception {
        NewsTestData.insertCompleteDraft(jdbcTemplate, NEWS_ID, "a-complete-news");
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);

        HttpResponse<String> published = browser.post(
                CONTENT + "/" + NEWS_ID + "/publish", null, token, "\"1\"");

        assertThat(published.statusCode()).isEqualTo(200);
        assertThat(published.headers().firstValue("ETag")).contains("\"2\"");
        assertNoStore(published);
        JsonNode body = JSON.readTree(published.body());
        assertThat(body.get("type").asString()).isEqualTo("news");
        assertThat(body.get("status").asString()).isEqualTo("PUBLISHED");

        HttpResponse<String> unpublished = browser.post(
                CONTENT + "/" + NEWS_ID + "/unpublish", null, token,
                published.headers().firstValue("ETag").orElseThrow());

        assertThat(unpublished.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(unpublished.body()).get("status").asString())
                .isEqualTo("UNPUBLISHED");
    }
}
