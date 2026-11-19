package vn.io.ajarline.content.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.LearningTestData.MODULE_A2;
import static vn.io.ajarline.support.LearningTestData.MODULE_A3;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
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
import vn.io.ajarline.publication.contract.ContentPublisher;
import vn.io.ajarline.publication.contract.ContentUnpublisher;
import vn.io.ajarline.support.ContentTestData;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class
})
class StandaloneArticleLearningApiIT extends AbstractAdminAuthenticationIT {

    private static final String STANDALONE = "/api/v1/articles?standalone=true";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ContentPublisher publisher;

    @Autowired
    private ContentUnpublisher unpublisher;

    @BeforeEach
    void insertScenarios() {
        removeRows();
        LearningTestData.insertStandardScenarios(jdbc);
    }

    @AfterEach
    void removeRows() {
        LearningTestData.deleteAll(jdbc);
    }

    @Test
    void list_ShouldReturnArticlesOutsidePublicModules_WhenSomeModulesAreHidden() throws Exception {
        // A5 is in a draft Module and B1 in a draft Module, so readers see both as standalone.
        assertThat(slugsOf(page(""))).containsExactly("article-a5", "article-b1", "article-e");
    }

    @Test
    void list_ShouldFillEveryPage_WhenPublicModuleArticlesSitBetweenStandaloneOnes() throws Exception {
        JsonNode first = page("&size=2");
        JsonNode second = page("&page=1&size=2");

        assertThat(slugsOf(first)).containsExactly("article-a5", "article-b1");
        assertThat(first.get("hasMore").asBoolean()).isTrue();
        assertThat(slugsOf(second)).containsExactly("article-e");
        assertThat(second.get("hasMore").asBoolean()).isFalse();
    }

    @Test
    void list_ShouldStillWork_WhenNoModuleExists() throws Exception {
        LearningTestData.deleteAll(jdbc);
        ContentTestData.insertArticle(jdbc, uuid(1), "first-one", "First", PublicationStatus.PUBLISHED);
        ContentTestData.insertArticle(jdbc, uuid(2), "second-one", "Second", PublicationStatus.PUBLISHED);

        assertThat(slugsOf(page(""))).containsExactly("first-one", "second-one");
    }

    @Test
    void list_ShouldDropArticle_WhenItsModuleBecomesPublic() throws Exception {
        publisher.publish(MODULE_A3, 1);

        assertThat(slugsOf(page(""))).containsExactly("article-b1", "article-e");
    }

    @Test
    void list_ShouldBringArticleBack_WhenItsModuleIsUnpublished() throws Exception {
        unpublisher.unpublish(MODULE_A2, 1);

        assertThat(slugsOf(page(""))).containsExactly(
                "article-a4", "article-a5", "article-b1", "article-e");
    }

    private JsonNode page(String query) throws Exception {
        HttpResponse<String> response = new Browser().get(STANDALONE + query);
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body());
    }

    private static List<String> slugsOf(JsonNode page) {
        List<String> slugs = new ArrayList<>();
        page.get("items").forEach(item -> slugs.add(item.get("slug").asString()));
        return slugs;
    }
}
