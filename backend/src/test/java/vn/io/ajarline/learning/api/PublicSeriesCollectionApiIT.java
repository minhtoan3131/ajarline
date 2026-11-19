package vn.io.ajarline.learning.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.LearningTestData.SERIES_A;
import static vn.io.ajarline.support.TopicTestData.insertAssignment;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.administration.api.AbstractAdminAuthenticationIT;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class
})
class PublicSeriesCollectionApiIT extends AbstractAdminAuthenticationIT {

    private static final String SERIES = "/api/v1/series";

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void removeRowsBeforeTest() {
        removeRows();
    }

    @AfterEach
    void removeRows() {
        LearningTestData.deleteAll(jdbc);
        jdbc.update("DELETE FROM topic");
    }

    @Test
    void list_ShouldReturnExactContract_WhenSeriesIsPublic() throws Exception {
        LearningTestData.insertStandardScenarios(jdbc);
        insertTopic(jdbc, uuid(1), "java", "Java", true);
        insertAssignment(jdbc, SERIES_A, uuid(1));

        HttpResponse<String> response = new Browser().get(SERIES);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/json");
        JsonNode body = JSON.readTree(response.body());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("page", "size", "hasMore", "items");
        assertThat(body.get("page").asInt()).isZero();
        assertThat(body.get("size").asInt()).isEqualTo(20);
        assertThat(body.get("hasMore").asBoolean()).isFalse();
        JsonNode item = body.get("items").get(0);
        assertThat(item.propertyNames()).containsExactlyInAnyOrder(
                "id", "type", "slug", "title", "summary", "topics", "publishedAt", "updatedAt");
        assertThat(item.get("type").asString()).isEqualTo("series");
        assertThat(item.get("slug").asString()).isEqualTo("series-a");
        assertThat(item.get("title").asString()).isEqualTo("Fixture series-a");
        assertThat(item.get("summary").asString()).isEqualTo("Fixture summary");
        assertThat(item.get("topics").get(0).propertyNames())
                .containsExactlyInAnyOrder("id", "slug", "name");
        assertThat(item.get("topics").get(0).get("slug").asString()).isEqualTo("java");
    }

    @Test
    void list_ShouldReturnOnlyPublicSeries_WhenOthersAreHidden() throws Exception {
        LearningTestData.insertStandardScenarios(jdbc);

        assertThat(slugsOf(page(""))).containsExactly("series-a");
    }

    @Test
    void list_ShouldReturnEmptyPage_WhenNoSeriesIsPublic() throws Exception {
        JsonNode body = page("");

        assertThat(body.get("items").isEmpty()).isTrue();
        assertThat(body.get("hasMore").asBoolean()).isFalse();
    }

    @Test
    void list_ShouldReturnEmptyPage_WhenPageIsAfterTheLast() throws Exception {
        LearningTestData.insertStandardScenarios(jdbc);

        JsonNode body = page("?page=5");

        assertThat(body.get("items").isEmpty()).isTrue();
        assertThat(body.get("hasMore").asBoolean()).isFalse();
    }

    @Test
    void list_ShouldOrderByIdAfterTitle_WhenTitlesAreEqual() throws Exception {
        // The Series with the larger id is inserted first, so the test proves the sort.
        LearningTestData.insertPublicSeries(jdbc, 3010, "same-two", "Same");
        LearningTestData.insertPublicSeries(jdbc, 3000, "same-one", "Same");

        assertThat(slugsOf(page(""))).containsExactly("same-one", "same-two");
    }

    @Test
    void list_ShouldFillEveryPage_WhenHiddenSeriesSitsAtThePageEdge() throws Exception {
        LearningTestData.insertPublicSeries(jdbc, 3000, "page-a", "A");
        // Published, but with no Module. Readers cannot see it.
        LearningTestData.insertSeries(jdbc, uuid(3010), "page-b", PublicationStatus.PUBLISHED, "# Overview");
        jdbc.update("UPDATE content_item SET title = 'B' WHERE id = ?", uuid(3010));
        LearningTestData.insertPublicSeries(jdbc, 3020, "page-c", "C");
        LearningTestData.insertPublicSeries(jdbc, 3030, "page-d", "D");

        JsonNode first = page("?size=2");
        JsonNode second = page("?page=1&size=2");

        assertThat(slugsOf(first)).containsExactly("page-a", "page-c");
        assertThat(first.get("hasMore").asBoolean()).isTrue();
        assertThat(slugsOf(second)).containsExactly("page-d");
        assertThat(second.get("hasMore").asBoolean()).isFalse();
    }

    @ParameterizedTest(name = "[{index}] {0} gives {1}")
    @CsvSource({
            "page=-1,   INVALID_PAGE",
            "page=abc,  INVALID_PAGE",
            "size=0,    INVALID_PAGE_SIZE",
            "size=51,   INVALID_PAGE_SIZE",
            "size=many, INVALID_PAGE_SIZE"
    })
    void list_ShouldReturnBadRequest_WhenPageOrSizeIsInvalid(String query, String code) throws Exception {
        HttpResponse<String> response = new Browser().get(SERIES + "?" + query);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JSON.readTree(response.body()).get("code").asString()).isEqualTo(code);
    }

    private JsonNode page(String query) throws Exception {
        HttpResponse<String> response = new Browser().get(SERIES + query);
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body());
    }

    private static List<String> slugsOf(JsonNode page) {
        List<String> slugs = new ArrayList<>();
        page.get("items").forEach(item -> slugs.add(item.get("slug").asString()));
        return slugs;
    }
}
