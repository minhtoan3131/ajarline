package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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
class AdminContentListApiIT extends AbstractAdminAuthenticationIT {

    private static final String CONTENT = "/api/admin/v1/content";

    private static final UUID SERIES_1 = uuid(1);
    private static final UUID SERIES_2 = uuid(2);
    private static final UUID SERIES_3 = uuid(3);
    private static final UUID MODULE = uuid(4);
    private static final UUID ARTICLE = uuid(5);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void insertRows() {
        LearningTestData.deleteAll(jdbcTemplate);
        LearningTestData.insertSeries(jdbcTemplate, SERIES_1, "alpha-series",
                PublicationStatus.DRAFT, "Fixture overview");
        LearningTestData.insertSeries(jdbcTemplate, SERIES_2, "beta-series",
                PublicationStatus.PUBLISHED, "Fixture overview");
        LearningTestData.insertSeries(jdbcTemplate, SERIES_3, "gamma-series",
                PublicationStatus.UNPUBLISHED, "Fixture overview");
        LearningTestData.insertModule(jdbcTemplate, MODULE, "a-module", SERIES_1, 1);
        ContentTestData.insertArticle(jdbcTemplate, ARTICLE, "an-article", "An article",
                PublicationStatus.DRAFT);
    }

    @AfterEach
    void removeRows() {
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @Test
    void list_ShouldReturnEverySeriesOfEveryStatus_WhenTypesIsSeries() throws Exception {
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.get(CONTENT + "?types=series");

        assertThat(response.statusCode()).isEqualTo(200);
        assertNoStore(response);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames())
                .containsExactlyInAnyOrder("filters", "page", "size", "hasMore", "items");
        assertThat(body.get("filters").propertyNames())
                .containsExactlyInAnyOrder("types", "statuses", "q");
        assertThat(body.get("filters").get("types")).hasSize(1);
        assertThat(body.get("filters").get("types").get(0).asString()).isEqualTo("series");
        assertThat(body.get("filters").get("statuses")).isEmpty();
        assertThat(body.get("filters").get("q").isNull()).isTrue();
        assertThat(body.get("page").asInt()).isZero();
        assertThat(body.get("size").asInt()).isEqualTo(20);
        assertThat(body.get("hasMore").asBoolean()).isFalse();

        JsonNode items = body.get("items");
        assertThat(items).hasSize(3);
        assertThat(items.get(0).propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "slug", "title", "status", "revision", "publishedAt", "updatedAt");
        assertThat(slugs(items)).containsExactlyInAnyOrder("alpha-series", "beta-series", "gamma-series");
        items.forEach(item -> assertThat(item.get("type").asString()).isEqualTo("series"));
        assertThat(statuses(items)).containsExactlyInAnyOrder("DRAFT", "PUBLISHED", "UNPUBLISHED");
    }

    @Test
    void list_ShouldOrderByUpdateTimeThenId_WhenItemsHaveSameTime() throws Exception {
        setUpdatedAt(SERIES_1, "2026-11-20T10:00:00Z");
        setUpdatedAt(SERIES_2, "2026-11-20T12:00:00Z");
        setUpdatedAt(SERIES_3, "2026-11-20T12:00:00Z");
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.get(CONTENT + "?types=series");

        // Newest first. Two items with the same time are ordered by id.
        assertThat(slugs(JSON.readTree(response.body()).get("items")))
                .containsExactly("beta-series", "gamma-series", "alpha-series");
    }

    @Test
    void list_ShouldSetHasMore_WhenTheNextPageHasAnItem() throws Exception {
        Browser browser = authenticatedBrowser();

        JsonNode exactFit = JSON.readTree(browser.get(CONTENT + "?types=series&size=3").body());
        JsonNode oneMore = JSON.readTree(browser.get(CONTENT + "?types=series&size=2").body());
        JsonNode secondPage = JSON.readTree(browser.get(CONTENT + "?types=series&size=2&page=1").body());

        assertThat(exactFit.get("items")).hasSize(3);
        assertThat(exactFit.get("hasMore").asBoolean()).isFalse();
        assertThat(oneMore.get("items")).hasSize(2);
        assertThat(oneMore.get("hasMore").asBoolean()).isTrue();
        assertThat(oneMore.get("size").asInt()).isEqualTo(2);
        assertThat(secondPage.get("items")).hasSize(1);
        assertThat(secondPage.get("hasMore").asBoolean()).isFalse();
        assertThat(secondPage.get("page").asInt()).isEqualTo(1);
    }

    @Test
    void list_ShouldReturnEmptyPage_WhenPageIsPastTheEnd() throws Exception {
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.get(CONTENT + "?types=series&size=2&page=9");

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.get("items")).isEmpty();
        assertThat(body.get("hasMore").asBoolean()).isFalse();
    }

    @Test
    void list_ShouldReturnEveryTypeOnePageAtATime_WhenNoFilterIsGiven() throws Exception {
        Browser browser = authenticatedBrowser();

        JsonNode body = JSON.readTree(browser.get(CONTENT).body());

        assertThat(body.get("items")).hasSize(5);
        assertThat(body.get("filters").get("types")).isEmpty();
    }

    @Test
    void list_ShouldFilterByStatus_WhenStatusesIsGiven() throws Exception {
        Browser browser = authenticatedBrowser();

        JsonNode body = JSON.readTree(
                browser.get(CONTENT + "?types=series&statuses=PUBLISHED,UNPUBLISHED").body());

        assertThat(slugs(body.get("items"))).containsExactlyInAnyOrder("beta-series", "gamma-series");
        assertThat(body.get("filters").get("statuses")).hasSize(2);
    }

    @Test
    void list_ShouldMatchTitleOrSlugIgnoringCase_WhenTextIsGiven() throws Exception {
        Browser browser = authenticatedBrowser();

        JsonNode bySlug = JSON.readTree(browser.get(CONTENT + "?q=%20BETA%20").body());
        JsonNode byTitle = JSON.readTree(browser.get(CONTENT + "?q=An%20Article").body());

        assertThat(slugs(bySlug.get("items"))).containsExactly("beta-series");
        assertThat(bySlug.get("filters").get("q").asString()).isEqualTo("BETA");
        assertThat(slugs(byTitle.get("items"))).containsExactly("an-article");
    }

    @Test
    void list_ShouldTreatTextAsLiteral_WhenItHasAWildcardCharacter() throws Exception {
        Browser browser = authenticatedBrowser();

        // A percent sign is not a wildcard here, and no title or slug has one.
        JsonNode body = JSON.readTree(browser.get(CONTENT + "?q=%25").body());

        assertThat(body.get("items")).isEmpty();
    }

    @Test
    void list_ShouldMatchOnlyItemsWithAPercentSign_WhenTextIsAPercentSign() throws Exception {
        ContentTestData.insertArticle(jdbcTemplate, uuid(6), "growth-tips",
                "Growth of 100% in speed", PublicationStatus.DRAFT);
        Browser browser = authenticatedBrowser();

        JsonNode body = JSON.readTree(browser.get(CONTENT + "?q=%25").body());

        assertThat(slugs(body.get("items"))).containsExactly("growth-tips");
    }

    @Test
    void list_ShouldMatchOnlyItemsWithAnUnderscore_WhenTextIsAnUnderscore() throws Exception {
        ContentTestData.insertArticle(jdbcTemplate, uuid(7), "snake-case-tips",
                "Use snake_case names", PublicationStatus.DRAFT);
        Browser browser = authenticatedBrowser();

        JsonNode body = JSON.readTree(browser.get(CONTENT + "?q=_").body());

        assertThat(slugs(body.get("items"))).containsExactly("snake-case-tips");
    }

    @Test
    void list_ShouldMatchTheSlug_WhenTheTitleDoesNotHaveTheText() throws Exception {
        ContentTestData.insertArticle(jdbcTemplate, uuid(8), "only-in-slug",
                "Zeta", PublicationStatus.DRAFT);
        Browser browser = authenticatedBrowser();

        JsonNode body = JSON.readTree(browser.get(CONTENT + "?q=only-in").body());

        assertThat(slugs(body.get("items"))).containsExactly("only-in-slug");
    }

    @Test
    void list_ShouldReturnEmptyPage_WhenNoNewsExists() throws Exception {
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.get(CONTENT + "?types=news");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(response.body()).get("items")).isEmpty();
    }

    @Test
    void list_ShouldReturnBadRequest_WhenATypeIsUnknown() throws Exception {
        Browser browser = authenticatedBrowser();

        HttpResponse<String> response = browser.get(CONTENT + "?types=book");

        assertProblem(response, 400, "INVALID_RESULT_TYPE");
    }

    private void setUpdatedAt(UUID id, String time) {
        jdbcTemplate.update("UPDATE content_item SET updated_at = ? WHERE id = ?",
                OffsetDateTime.parse(time), id);
    }

    private static List<String> slugs(JsonNode items) {
        List<String> slugs = new ArrayList<>();
        items.forEach(item -> slugs.add(item.get("slug").asString()));
        return slugs;
    }

    private static List<String> statuses(JsonNode items) {
        List<String> statuses = new ArrayList<>();
        items.forEach(item -> statuses.add(item.get("status").asString()));
        return statuses;
    }
}
