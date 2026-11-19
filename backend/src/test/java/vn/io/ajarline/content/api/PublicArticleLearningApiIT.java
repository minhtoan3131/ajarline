package vn.io.ajarline.content.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.administration.api.AbstractAdminAuthenticationIT;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class
})
class PublicArticleLearningApiIT extends AbstractAdminAuthenticationIT {

    private static final String ARTICLES = "/api/v1/articles/";

    @Autowired
    private JdbcTemplate jdbc;

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
    void get_ShouldGiveModuleSeriesAndNeighbours_WhenArticleIsInPublicModule() throws Exception {
        JsonNode body = article("article-a3");

        assertThat(body.get("module").get("slug").asString()).isEqualTo("module-a1");
        assertThat(body.get("module").get("title").asString()).isEqualTo("Fixture module-a1");
        assertThat(body.get("module").get("position").asInt()).isEqualTo(1);
        assertThat(body.get("series").get("slug").asString()).isEqualTo("series-a");
        // Article A2 is a draft, so the nearest public Article before A3 is A1.
        assertThat(body.get("previousArticle").get("slug").asString()).isEqualTo("article-a1");
        assertThat(body.get("previousArticle").get("title").asString()).isEqualTo("Article article-a1");
        assertThat(body.get("previousArticle").get("position").asInt()).isEqualTo(1);
        assertThat(body.get("nextArticle").isNull()).isTrue();
    }

    @Test
    void get_ShouldHaveNoPrevious_WhenArticleIsFirstInModule() throws Exception {
        JsonNode body = article("article-a1");

        assertThat(body.get("previousArticle").isNull()).isTrue();
        assertThat(body.get("nextArticle").get("slug").asString()).isEqualTo("article-a3");
        assertThat(body.get("nextArticle").get("position").asInt()).isEqualTo(2);
    }

    @Test
    void get_ShouldHaveNoNeighbour_WhenArticleIsAloneInItsModule() throws Exception {
        JsonNode body = article("article-a4");

        assertThat(body.get("previousArticle").isNull()).isTrue();
        assertThat(body.get("nextArticle").isNull()).isTrue();
    }

    @Test
    void get_ShouldRankModuleAmongPublicModules_WhenAModuleBeforeItIsHidden() throws Exception {
        JsonNode body = article("article-a4");

        // Module A3 is a draft between A1 and A2, so A2 has rank 2, not the stored position 3.
        assertThat(body.get("module").get("slug").asString()).isEqualTo("module-a2");
        assertThat(body.get("module").get("position").asInt()).isEqualTo(2);
    }

    @Test
    void get_ShouldHaveNoSeries_WhenOnlyTheSeriesIsHidden() throws Exception {
        JsonNode body = article("article-c1");

        assertThat(body.get("module").get("slug").asString()).isEqualTo("module-c1");
        assertThat(body.get("module").get("position").asInt()).isEqualTo(1);
        assertThat(body.get("series").isNull()).isTrue();
    }

    @Test
    void get_ShouldHaveNoContext_WhenModuleIsHidden() throws Exception {
        JsonNode body = article("article-a5");

        assertThat(body.get("module").isNull()).isTrue();
        assertThat(body.get("series").isNull()).isTrue();
        assertThat(body.get("previousArticle").isNull()).isTrue();
        assertThat(body.get("nextArticle").isNull()).isTrue();
    }

    @Test
    void get_ShouldHaveNoContext_WhenArticleIsStandalone() throws Exception {
        JsonNode body = article("article-e");

        assertThat(body.get("module").isNull()).isTrue();
        assertThat(body.get("series").isNull()).isTrue();
        assertThat(body.get("previousArticle").isNull()).isTrue();
        assertThat(body.get("nextArticle").isNull()).isTrue();
    }

    @Test
    void get_ShouldNotShowAnyHiddenSlugOrTitle_WhenParentsOrNeighboursAreHidden() throws Exception {
        assertThat(rawArticle("article-a5")).doesNotContain("module-a3", "series-a");
        assertThat(rawArticle("article-c1")).doesNotContain("series-c");
        assertThat(rawArticle("article-a3")).doesNotContain("article-a2", "module-a3");
        assertThat(rawArticle("article-a4")).doesNotContain("module-a3");
    }

    private JsonNode article(String slug) throws Exception {
        return JSON.readTree(rawArticle(slug));
    }

    private String rawArticle(String slug) throws Exception {
        HttpResponse<String> response = new Browser().get(ARTICLES + slug);
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }
}
