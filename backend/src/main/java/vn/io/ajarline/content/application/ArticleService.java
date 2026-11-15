package vn.io.ajarline.content.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
public class ArticleService implements ArticleCreator, ArticleReader, ArticleEditor, ArticlePublicationAccess {
    private static final String SLUG_CONSTRAINT = "uk_content_item_type_slug";
    private final ContentItemJpaRepository contentRepository;
    private final ArticleJpaRepository articleRepository;
    private final ArticlePolicy policy;
    private final Clock clock;
    private final TopicAssignmentPort topicAssignment;

    public ArticleService(ContentItemJpaRepository contentRepository,
                          ArticleJpaRepository articleRepository, ArticlePolicy policy, Clock clock,
                          TopicAssignmentPort topicAssignment) {
        this.contentRepository = contentRepository;
        this.articleRepository = articleRepository;
        this.policy = policy;
        this.clock = clock;
        this.topicAssignment = topicAssignment;
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
        flushCanonicalWrite();
        // The content row must exist before an assignment can point at it.
        topicAssignment.replaceAssignments(id, normalizedInput.topicIds());
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
    public Optional<PublicArticleView> findPublishedBySlug(String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        return contentRepository.findByTypeAndSlugAndStatus(
                        ContentType.ARTICLE,
                        slug,
                        PublicationStatus.PUBLISHED)
                .map(content -> new PublicArticleView(
                        toView(content, requireArticle(content.getId())),
                        topicAssignment.topicLabelsOf(content.getId())));
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
    private void flushCanonicalWrite() {
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
                content.getUpdatedAt(),
                topicAssignment.topicIdsOf(content.getId())
        );
    }

    private ContentOperationException notFound(UUID id) {
        return new ContentOperationException(ContentFailureCode.CONTENT_NOT_FOUND,
                "Article was not found: " + id);
    }

    @Override
    @Transactional
    public ArticleView replace(UUID id, long expectedRevision, ArticleContentInput input) {
        ArticleContentInput normalizedInput = policy.normalizeAndValidate(input);
        LockedArticle locked = lockArticle(id, expectedRevision);
        ContentItemEntity contentItemEntity = locked.content();
        ArticleEntity articleEntity = locked.article();

        if (contentItemEntity.getRevision() != expectedRevision) {
            throw new ContentOperationException(
                    ContentFailureCode.REVISION_CONFLICT,
                    "Article revision does not match the expected revision."
            );
        }

        boolean hasSlugChanged = !contentItemEntity.getSlug().equals(normalizedInput.slug());
        if (contentItemEntity.hasEverBeenPublished() && hasSlugChanged) {
            throw new ContentOperationException(
                    ContentFailureCode.SLUG_IMMUTABLE_AFTER_PUBLICATION,
                    "Slug cannot change after the first publication."
            );
        }

        boolean hasCommonChanged = !contentItemEntity.hasSameEditableContent(
                normalizedInput.title(),
                normalizedInput.slug(),
                normalizedInput.summary()
        );

        boolean hasBodyChanged = !articleEntity.hasSameBody(normalizedInput.bodyMarkdown());
        // The assignment is written here, before any decision to return early, so
        // that one failure rolls back the whole save.
        boolean hasTopicsChanged = topicAssignment.replaceAssignments(id, normalizedInput.topicIds());
        if (!hasCommonChanged && !hasBodyChanged && !hasTopicsChanged) {
            return toView(contentItemEntity, articleEntity);
        }

        Instant now = clock.instant();
        contentItemEntity.replaceEditTableContent(
                normalizedInput.title(),
                normalizedInput.slug(),
                normalizedInput.summary(),
                now
        );

        articleEntity.replaceBody(normalizedInput.bodyMarkdown());
        flushCanonicalWrite();
        return toView(contentItemEntity, articleEntity);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ArticleView lockForPublication(UUID id, long expectedRevision) {
        LockedArticle locked = lockArticle(id, expectedRevision);
        return toView(locked.content(), locked.article());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ArticleView markPublished(
            UUID id,
            long expectedRevision,
            Instant changedAt
    ) {
        LockedArticle locked = lockArticle(id, expectedRevision);
        locked.content().markPublished(changedAt);
        flushCanonicalWrite();
        return toView(locked.content(), locked.article());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ArticleView markUnpublished(
            UUID id,
            long expectedRevision,
            Instant changedAt
    ) {
        LockedArticle locked = lockArticle(id, expectedRevision);
        locked.content().markUnpublished(changedAt);
        flushCanonicalWrite();
        return toView(locked.content(), locked.article());
    }


    private LockedArticle lockArticle(UUID id, long expectedRevision) {
        ContentItemEntity content = contentRepository.findByIdForUpdate(id)
                .filter(item -> item.getType() == ContentType.ARTICLE)
                .orElseThrow(() -> notFound(id));
        ArticleEntity article = requireArticle(id);
        requireExpectedRevision(content, expectedRevision);
        return new LockedArticle(content, article);
    }

    private void requireExpectedRevision(
            ContentItemEntity content,
            long expectedRevision
    ) {
        if (content.getRevision() != expectedRevision) {
            throw new ContentOperationException(
                    ContentFailureCode.REVISION_CONFLICT,
                    "Article revision does not match the expected revision.");
        }
    }
    private record LockedArticle(ContentItemEntity content, ArticleEntity article) {}
}
