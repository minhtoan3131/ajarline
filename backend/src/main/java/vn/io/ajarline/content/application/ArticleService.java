package vn.io.ajarline.content.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.*;
import vn.io.ajarline.content.infrastructure.persistence.ArticleEntity;
import vn.io.ajarline.content.infrastructure.persistence.ArticleJpaRepository;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemEntity;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemJpaRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class ArticleService implements ArticleCreator, ArticleReader {
    private static final String SLUG_CONSTRAINT = "uk_content_item_type_slug";
    private final ContentItemJpaRepository contentRepository;
    private final ArticleJpaRepository articleRepository;
    private final ArticlePolicy policy;
    private final Clock clock;

    public ArticleService(ContentItemJpaRepository contentRepository,
                          ArticleJpaRepository articleRepository, ArticlePolicy policy, Clock clock) {
        this.contentRepository = contentRepository;
        this.articleRepository = articleRepository;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ArticleView create(ArticleContentInput input) {
        ArticleContentInput normalizedInput = policy.normalizeAndValidate(input);
        UUID id = UUID.randomUUID();
        Instant now = clock.instant();

        ContentItemEntity contentItemEntity = ContentItemEntity.draftArticle(id, normalizedInput.slug(),
                normalizedInput.title(), normalizedInput.summary(), now);
        ArticleEntity articleEntity = new ArticleEntity(id, normalizedInput.bodyMarkdown());

        contentRepository.save(contentItemEntity);
        articleRepository.save(articleEntity);
        flushCanonicalWrite(contentRepository, articleRepository);
        return toView(contentItemEntity, articleEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleView get(UUID id) {
        ContentItemEntity contentItemEntity = contentRepository.findById(id)
                .filter(contentItem -> contentItem.getType() == ContentType.ARTICLE)
                .orElseThrow(() -> notFound(id));
        ArticleEntity articleEntity = requireArticle(id);
        return toView(contentItemEntity, articleEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ArticleView> findPublishedBySlug(String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        String normalizedSlug = slug.strip().toLowerCase(Locale.ROOT);
        return contentRepository.findByTypeAndSlugAndStatus(
                ContentType.ARTICLE,
                normalizedSlug,
                PublicationStatus.PUBLISHED
        ).map(contentItem -> toView(contentItem, requireArticle(contentItem.getId())));

    }

    private ArticleEntity requireArticle(UUID uuid) {
        return articleRepository.findById(uuid).orElseThrow(
                () -> new IllegalArgumentException("Article subtype row is missing for " + uuid)
        );
    }

    /**
     * Flushes Article changes to detect database errors before commit.
     * Converts slug conflicts into content errors.
     */
    private void flushCanonicalWrite(ContentItemJpaRepository contentRepository,
                                     ArticleJpaRepository articleRepository) {
        try {
            contentRepository.flush();
            articleRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, SLUG_CONSTRAINT)) {
                throw new ContentOperationException(
                        ContentFailureCode.SLUG_CONFLICT,
                        "An Article with this slug already exists.",
                        exception
                );
            }
            throw exception;
        }
    }

    private boolean containsConstraint(Throwable failure, String constraint) {
        Throwable current = failure;
        while (current != null) {
            if (current.getMessage() != null
                    && current.getMessage().contains(constraint)
            ) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ArticleView toView(ContentItemEntity content, ArticleEntity article) {
        return new ArticleView(
                content.getId(),
                content.getType(),
                content.getTitle(),
                content.getSlug(),
                content.getSummary(),
                article.getBodyMarkdown(),
                content.getStatus(),
                content.getRevision(),
                content.getPublishedAt(),
                content.getUnpublishedAt(),
                content.getCreatedAt(),
                content.getUpdatedAt()
        );
    }

    private ContentOperationException notFound(UUID id) {
        return new ContentOperationException(ContentFailureCode.CONTENT_NOT_FOUND,
                "Article was not found: " + id);
    }
}
