package vn.io.ajarline.content.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
public class ArticlePersistenceIT {
    private static final UUID CONTENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final Instant NOW = Instant.parse("2026-10-26T00:00:00Z");

    @Autowired
    private ContentItemJpaRepository contentItemJpaRepository;

    @Autowired
    private ArticleJpaRepository articleJpaRepository;

    @Test
    @Transactional
    void save_ShouldPersistCommonAndSubtypeRows_WhenArticleIsInvalid() {
        ContentItemEntity content = ContentItemEntity.draftArticle(CONTENT_ID,
                "java-threads", "Java threads", null, NOW);
        ArticleEntity article = new ArticleEntity(CONTENT_ID, "# Java threads");

        contentItemJpaRepository.save(content);
        articleJpaRepository.save(article);
        articleJpaRepository.flush();

        ArticleEntity persistedArticle = articleJpaRepository.findById(CONTENT_ID).orElseThrow();
        assertThat(contentItemJpaRepository.findById(CONTENT_ID)).isPresent();
        assertThat(persistedArticle.getBodyMarkdown()).isEqualTo("# Java threads");
    }


}
