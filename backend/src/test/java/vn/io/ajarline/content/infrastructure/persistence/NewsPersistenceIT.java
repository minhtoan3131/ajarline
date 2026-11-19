package vn.io.ajarline.content.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
class NewsPersistenceIT {

    private static final UUID CONTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000502");
    private static final Instant NOW = Instant.parse("2026-11-19T00:00:00Z");
    private static final Instant SOURCE_TIME = Instant.parse("2026-11-18T03:00:00Z");

    @Autowired
    private ContentItemJpaRepository contentRepository;

    @Autowired
    private NewsJpaRepository newsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    void save_ShouldPersistCommonAndNewsRows_WhenNewsIsComplete() {
        contentRepository.save(ContentItemEntity.draft(
                CONTENT_ID, ContentType.NEWS, "a-news-item", "A news item", null, NOW));
        newsRepository.save(new NewsEntity(
                CONTENT_ID, "# Body", "A source", "https://example.com/story", SOURCE_TIME));
        entityManager.flush();
        // Clear the persistence context, so the next read goes to the database.
        entityManager.clear();

        NewsEntity saved = newsRepository.findById(CONTENT_ID).orElseThrow();

        assertThat(saved.getBodyMarkdown()).isEqualTo("# Body");
        assertThat(saved.getSourceName()).isEqualTo("A source");
        assertThat(saved.getSourceUrl()).isEqualTo("https://example.com/story");
        assertThat(saved.getSourcePublishedAt()).isEqualTo(SOURCE_TIME);
    }
}
