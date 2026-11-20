package vn.io.ajarline.topic.api;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.ContentTestData.insertArticle;
import static vn.io.ajarline.support.NewsTestData.insertNews;
import static vn.io.ajarline.support.TopicTestData.insertAssignment;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import vn.io.ajarline.administration.api.AbstractAdminAuthenticationIT;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({
        PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class
})
class PublicTopicNewsApiIT extends AbstractAdminAuthenticationIT {

    private static final String TOPICS = "/api/v1/topics";
    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2026-11-12T00:00:00Z");
    private static final UUID JAVA = uuid(1);
    private static final UUID NEWS_ONLY = uuid(2);

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        jdbc.update("DELETE FROM content_item");
        jdbc.update("DELETE FROM topic");
        insertTopic(jdbc, JAVA, "java", "Java", true);
        insertTopic(jdbc, NEWS_ONLY, "news-only", "News only", true);
    }

    @Test
    void content_ShouldListNewsInTheSameOrderAsTheNewsList_WhenTimesAreEqual() throws Exception {
        OffsetDateTime sameTime = BASE_TIME.minusDays(1);
        // Two items share a time, so the id decides their place in both lists.
        insertAssigned(uuid(4), "tie-b", sameTime);
        insertAssigned(uuid(1001), "oldest", BASE_TIME.minusDays(2));
        insertAssigned(uuid(3), "tie-a", sameTime);
        insertAssigned(uuid(1002), "newest", BASE_TIME);
        insertArticle(jdbc, uuid(5), "an-article", "An Article", PublicationStatus.PUBLISHED);
        insertAssignment(jdbc, uuid(5), JAVA);
        Browser reader = new Browser();

        List<String> newsList = slugs(JSON.readTree(
                reader.get("/api/v1/news?size=50").body()).get("items"));
        List<String> topicNews = new ArrayList<>();
        for (JsonNode item : JSON.readTree(
                reader.get(TOPICS + "/java/content?size=50").body()).get("items")) {
            if ("news".equals(item.get("type").asString())) {
                topicNews.add(item.get("slug").asString());
            }
        }

        assertThat(newsList).containsExactly("newest", "tie-a", "tie-b", "oldest");
        assertThat(topicNews).isEqualTo(newsList);
    }

    @Test
    void topic_ShouldBePublic_WhenItOnlyHasPublishedNews() throws Exception {
        insertNews(jdbc, uuid(1), "only-news", PublicationStatus.PUBLISHED,
                "A summary", "# Body", "Source", "https://example.com/a");
        insertAssignment(jdbc, uuid(1), NEWS_ONLY);
        Browser reader = new Browser();

        JsonNode topics = JSON.readTree(reader.get(TOPICS).body()).get("topics");
        assertThat(slugs(topics)).containsExactly("news-only");
        assertThat(reader.get(TOPICS + "/news-only").statusCode()).isEqualTo(200);

        JsonNode items = JSON.readTree(reader.get(TOPICS + "/news-only/content").body()).get("items");
        assertThat(items.size()).isEqualTo(1);
        assertThat(items.get(0).get("type").asString()).isEqualTo("news");
        assertThat(items.get(0).get("slug").asString()).isEqualTo("only-news");
        assertThat(items.get(0).get("context").get("series").isNull()).isTrue();
        assertThat(items.get(0).get("context").get("module").isNull()).isTrue();

        jdbc.update("""
                UPDATE content_item SET status = 'UNPUBLISHED', unpublished_at = published_at
                 WHERE id = ?
                """, uuid(1));

        assertThat(reader.get(TOPICS + "/news-only").statusCode()).isEqualTo(404);
        assertThat(slugs(JSON.readTree(reader.get(TOPICS).body()).get("topics"))).isEmpty();
    }

    private void insertAssigned(UUID id, String slug, OffsetDateTime publishedAt) {
        insertNews(jdbc, id, slug, PublicationStatus.PUBLISHED,
                "Summary of " + slug, "# Body", "Source", "https://example.com/" + slug);
        jdbc.update("UPDATE content_item SET published_at = ? WHERE id = ?", publishedAt, id);
        insertAssignment(jdbc, id, JAVA);
    }

    private static List<String> slugs(JsonNode array) {
        List<String> slugs = new ArrayList<>();
        for (JsonNode element : array) {
            slugs.add(element.get("slug").asString());
        }
        return slugs;
    }
}
