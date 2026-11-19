package vn.io.ajarline.learning.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.io.ajarline.content.contract.PublicationStatus.DRAFT;
import static vn.io.ajarline.content.contract.PublicationStatus.PUBLISHED;
import static vn.io.ajarline.support.LearningTestData.ARTICLE_D1;
import static vn.io.ajarline.support.LearningTestData.MODULE_B1;
import static vn.io.ajarline.support.LearningTestData.MODULE_D1;
import static vn.io.ajarline.support.LearningTestData.SERIES_B;
import static vn.io.ajarline.support.TopicTestData.insertAssignment;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import vn.io.ajarline.publication.contract.ContentPublisher;
import vn.io.ajarline.publication.contract.PublicationFailureCode;
import vn.io.ajarline.publication.contract.PublicationOperationException;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class
})
class PublicVisibilityAgreementIT extends AbstractAdminAuthenticationIT {

    private static final List<String> ALL_SERIES =
            List.of("series-a", "series-b", "series-c", "series-d");
    private static final Set<String> PUBLIC_SERIES = Set.of("series-a");
    private static final List<String> ALL_MODULES =
            List.of("module-a1", "module-a2", "module-a3", "module-b1", "module-c1", "module-d1");
    private static final Set<String> PUBLIC_MODULES = Set.of("module-a1", "module-a2", "module-c1");
    /** The public Articles in a public Module, with the Module that readers see. */
    private static final Map<String, String> MODULE_OF_ARTICLE = Map.of(
            "article-a1", "module-a1",
            "article-a3", "module-a1",
            "article-a4", "module-a2",
            "article-c1", "module-c1");
    private static final Set<String> STANDALONE = Set.of("article-a5", "article-b1", "article-e");

    private static final UUID EVERYTHING = uuid(1);
    private static final UUID HIDDEN_ONLY = uuid(2);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ContentPublisher publisher;

    @BeforeEach
    void insertScenariosAndTopics() {
        removeRows();
        LearningTestData.insertStandardScenarios(jdbc);
        insertTopic(jdbc, EVERYTHING, "agreement", "Agreement", true);
        insertTopic(jdbc, HIDDEN_ONLY, "hidden-only", "Hidden only", true);
        jdbc.queryForList("SELECT id FROM content_item", UUID.class)
                .forEach(id -> insertAssignment(jdbc, id, EVERYTHING));
        List.of(SERIES_B, MODULE_B1, MODULE_D1, ARTICLE_D1)
                .forEach(id -> insertAssignment(jdbc, id, HIDDEN_ONLY));
    }

    @AfterEach
    void removeRows() {
        LearningTestData.deleteAll(jdbc);
        jdbc.update("DELETE FROM topic");
    }

    @Test
    void everyReadPath_ShouldAgreeOnWhatReadersCanSee_WhenFixtureHasEverySituation() throws Exception {
        // R1: Series Index.
        assertThat(slugsOf(json("/api/v1/series?size=50").get("items"))).isEqualTo(sorted(PUBLIC_SERIES));

        // R2 and R3: one answer for every Series and every Module.
        for (String slug : ALL_SERIES) {
            assertThat(statusOf("/api/v1/series/" + slug))
                    .as(slug).isEqualTo(PUBLIC_SERIES.contains(slug) ? 200 : 404);
        }
        for (String slug : ALL_MODULES) {
            assertThat(statusOf("/api/v1/modules/" + slug))
                    .as(slug).isEqualTo(PUBLIC_MODULES.contains(slug) ? 200 : 404);
        }

        // R4: an Article shows its Module only when the Module is public.
        MODULE_OF_ARTICLE.forEach((article, module) -> {
            JsonNode body = jsonUnchecked("/api/v1/articles/" + article);
            assertThat(body.get("module").get("slug").asString()).as(article).isEqualTo(module);
        });
        for (String article : STANDALONE) {
            assertThat(jsonUnchecked("/api/v1/articles/" + article).get("module").isNull())
                    .as(article).isTrue();
        }

        // R5: Standalone Index.
        assertThat(slugsOf(json("/api/v1/articles?standalone=true&size=50").get("items")))
                .isEqualTo(sorted(STANDALONE));

        // R6: Topic Home shows the same Series and Modules, and puts exactly the standalone
        // Articles in the group of standalone Articles.
        JsonNode topicHome = json("/api/v1/topics/agreement/content?size=50");
        assertThat(slugsOfType(topicHome, "series")).isEqualTo(sorted(PUBLIC_SERIES));
        assertThat(slugsOfType(topicHome, "module")).isEqualTo(sorted(PUBLIC_MODULES));
        Set<String> standaloneInTopicHome = new HashSet<>();
        for (JsonNode item : topicHome.get("items")) {
            if (item.get("type").asString().equals("article") && item.get("context").get("module").isNull()) {
                standaloneInTopicHome.add(item.get("slug").asString());
            }
        }
        assertThat(standaloneInTopicHome).isEqualTo(STANDALONE);

        // Topic Index: a Topic with only hidden content is not listed.
        List<String> topics = new ArrayList<>();
        json("/api/v1/topics").get("topics").forEach(topic -> topics.add(topic.get("slug").asString()));
        assertThat(topics).containsExactly("agreement");
    }

    @Test
    void publishSeries_ShouldSucceedExactlyWhenTheSeriesWouldBeListed_WhenModulesDiffer() throws Exception {
        // Series X has a public Module. Series Y has a Module that is a draft.
        LearningTestData.insertSeries(jdbc, uuid(3100), "series-x", DRAFT, "# Overview");
        LearningTestData.insertModule(jdbc, uuid(3101), "module-x", uuid(3100), 1, PUBLISHED, "# Overview");
        LearningTestData.insertArticleIn(jdbc, uuid(3102), "article-x", PUBLISHED, uuid(3101), 1);
        LearningTestData.insertSeries(jdbc, uuid(3110), "series-y", DRAFT, "# Overview");
        LearningTestData.insertModule(jdbc, uuid(3111), "module-y", uuid(3110), 1, DRAFT, "# Overview");
        LearningTestData.insertArticleIn(jdbc, uuid(3112), "article-y", PUBLISHED, uuid(3111), 1);

        publisher.publish(uuid(3100), 1);
        assertThatThrownBy(() -> publisher.publish(uuid(3110), 1))
                .isInstanceOfSatisfying(PublicationOperationException.class, failure ->
                        assertThat(failure.code())
                                .isEqualTo(PublicationFailureCode.SERIES_HAS_NO_PUBLIC_MODULE));
        // Even if Series Y were published by other means, readers still cannot see it.
        jdbc.update("UPDATE content_item SET status = 'PUBLISHED', published_at = now() WHERE id = ?",
                uuid(3110));

        assertThat(slugsOf(json("/api/v1/series?size=50").get("items")))
                .containsExactly("series-a", "series-x");
        assertThat(statusOf("/api/v1/series/series-y")).isEqualTo(404);
    }

    private JsonNode json(String path) throws Exception {
        HttpResponse<String> response = new Browser().get(path);
        assertThat(response.statusCode()).as(path).isEqualTo(200);
        return JSON.readTree(response.body());
    }

    private JsonNode jsonUnchecked(String path) {
        try {
            return json(path);
        } catch (Exception failure) {
            throw new AssertionError("Could not read " + path, failure);
        }
    }

    private int statusOf(String path) throws Exception {
        return new Browser().get(path).statusCode();
    }

    private static List<String> slugsOf(JsonNode items) {
        List<String> slugs = new ArrayList<>();
        items.forEach(item -> slugs.add(item.get("slug").asString()));
        return slugs;
    }

    private static List<String> slugsOfType(JsonNode page, String type) {
        List<String> slugs = new ArrayList<>();
        for (JsonNode item : page.get("items")) {
            if (item.get("type").asString().equals(type)) {
                slugs.add(item.get("slug").asString());
            }
        }
        return slugs;
    }

    private static List<String> sorted(Set<String> slugs) {
        return slugs.stream().sorted().toList();
    }
}
