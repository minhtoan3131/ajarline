package vn.io.ajarline.topic.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.LearningTestData.ARTICLE_D1;
import static vn.io.ajarline.support.LearningTestData.MODULE_B1;
import static vn.io.ajarline.support.LearningTestData.MODULE_D1;
import static vn.io.ajarline.support.LearningTestData.SERIES_B;
import static vn.io.ajarline.support.TopicTestData.insertAssignment;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
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
import vn.io.ajarline.administration.api.AbstractAdminAuthenticationIT;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class
})
class PublicTopicLearningApiIT extends AbstractAdminAuthenticationIT {

    private static final String TOPICS = "/api/v1/topics";
    private static final UUID JAVA = uuid(1);
    private static final UUID HIDDEN_ONLY = uuid(2);

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepareTopicsAndHierarchy() {
        removeRows();
        LearningTestData.insertStandardScenarios(jdbc);
        insertTopic(jdbc, JAVA, "java", "Java", true);
        insertTopic(jdbc, HIDDEN_ONLY, "hidden-only", "Hidden only", true);
        assignEverythingToJava();
        List.of(SERIES_B, MODULE_B1, MODULE_D1, ARTICLE_D1)
                .forEach(id -> insertAssignment(jdbc, id, HIDDEN_ONLY));
    }

    @AfterEach
    void removeRows() {
        LearningTestData.deleteAll(jdbc);
        jdbc.update("DELETE FROM topic");
    }

    @Test
    void content_ShouldListOnlyPublicSeriesAndModules_WhenSomeAreHiddenByTheirChildren() throws Exception {
        assertThat(slugsOf(content("?types=series&size=50"))).containsExactly("series-a");
        assertThat(slugsOf(content("?types=module&size=50")))
                .containsExactly("module-a1", "module-a2", "module-c1");
    }

    @Test
    void content_ShouldGiveLearningContext_WhenArticlesAndModulesAreListed() throws Exception {
        JsonNode page = content("?size=50");

        JsonNode inModule = itemOf(page, "article-a3").get("context");
        assertThat(inModule.get("module").get("slug").asString()).isEqualTo("module-a1");
        assertThat(inModule.get("module").get("position").asInt()).isEqualTo(1);
        assertThat(inModule.get("series").get("slug").asString()).isEqualTo("series-a");
        // Module A3 is hidden between A1 and A2, so the rank of A2 is 2.
        assertThat(itemOf(page, "article-a4").get("context").get("module").get("position").asInt())
                .isEqualTo(2);
        JsonNode underHiddenSeries = itemOf(page, "article-c1").get("context");
        assertThat(underHiddenSeries.get("module").get("slug").asString()).isEqualTo("module-c1");
        assertThat(underHiddenSeries.get("series").isNull()).isTrue();
        assertThat(isStandalone(itemOf(page, "article-a5"))).isTrue();
        assertThat(isStandalone(itemOf(page, "article-b1"))).isTrue();
        assertThat(isStandalone(itemOf(page, "article-e"))).isTrue();
        JsonNode moduleWithSeries = itemOf(page, "module-a2").get("context");
        assertThat(moduleWithSeries.get("series").get("slug").asString()).isEqualTo("series-a");
        assertThat(moduleWithSeries.get("module").isNull()).isTrue();
        assertThat(isStandalone(itemOf(page, "module-c1"))).isTrue();
        assertThat(isStandalone(itemOf(page, "series-a"))).isTrue();
    }

    @Test
    void content_ShouldNotShowAnyHiddenSlugOrTitle_WhenContextIsBuilt() throws Exception {
        HttpResponse<String> response = new Browser().get(TOPICS + "/java/content?size=50");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).doesNotContain(
                "series-b", "series-c", "series-d",
                "module-a3", "module-b1", "module-d1",
                "article-a2", "article-d1");
    }

    @Test
    void get_ShouldReturnNotFound_WhenTopicHasOnlyHiddenLearningContent() throws Exception {
        assertThat(new Browser().get(TOPICS + "/hidden-only").statusCode()).isEqualTo(404);
        assertThat(new Browser().get(TOPICS + "/hidden-only/content").statusCode()).isEqualTo(404);
    }

    @Test
    void list_ShouldLeaveOutTopic_WhenItHasOnlyHiddenLearningContent() throws Exception {
        JsonNode body = JSON.readTree(new Browser().get(TOPICS).body());

        List<String> slugs = new ArrayList<>();
        body.get("topics").forEach(topic -> slugs.add(topic.get("slug").asString()));
        assertThat(slugs).containsExactly("java");
    }

    @Test
    void content_ShouldFillEveryPage_WhenHiddenSeriesSitsAtThePageEdge() throws Exception {
        LearningTestData.deleteAll(jdbc);
        LearningTestData.insertPublicSeries(jdbc, 3000, "page-a", "A");
        // Published, but with no Module. Readers cannot see it.
        LearningTestData.insertSeries(jdbc, uuid(3010), "page-b", PublicationStatus.PUBLISHED, "# Overview");
        jdbc.update("UPDATE content_item SET title = 'B' WHERE id = ?", uuid(3010));
        LearningTestData.insertPublicSeries(jdbc, 3020, "page-c", "C");
        LearningTestData.insertPublicSeries(jdbc, 3030, "page-d", "D");
        assignEverythingToJava();

        JsonNode first = content("?types=series&size=2");
        JsonNode second = content("?types=series&page=1&size=2");

        assertThat(slugsOf(first)).containsExactly("page-a", "page-c");
        assertThat(first.get("hasMore").asBoolean()).isTrue();
        assertThat(slugsOf(second)).containsExactly("page-d");
        assertThat(second.get("hasMore").asBoolean()).isFalse();
    }

    private void assignEverythingToJava() {
        jdbc.queryForList("SELECT id FROM content_item", UUID.class)
                .forEach(id -> insertAssignment(jdbc, id, JAVA));
    }

    private JsonNode content(String query) throws Exception {
        HttpResponse<String> response = new Browser().get(TOPICS + "/java/content" + query);
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body());
    }

    private static List<String> slugsOf(JsonNode page) {
        List<String> slugs = new ArrayList<>();
        page.get("items").forEach(item -> slugs.add(item.get("slug").asString()));
        return slugs;
    }

    private static JsonNode itemOf(JsonNode page, String slug) {
        for (JsonNode item : page.get("items")) {
            if (item.get("slug").asString().equals(slug)) {
                return item;
            }
        }
        throw new AssertionError("The page has no item with slug " + slug);
    }

    private static boolean isStandalone(JsonNode item) {
        JsonNode context = item.get("context");
        return context.get("series").isNull() && context.get("module").isNull();
    }
}
