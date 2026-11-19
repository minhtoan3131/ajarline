package vn.io.ajarline.content.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentEligibility;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.content.contract.ContentPublicationAccess;
import vn.io.ajarline.content.contract.ContentRecordInput;
import vn.io.ajarline.content.contract.ContentRecordReader;
import vn.io.ajarline.content.contract.ContentRecordSummary;
import vn.io.ajarline.content.contract.ContentRecordView;
import vn.io.ajarline.content.contract.ContentRecordWriter;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.EditableRecord;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.content.contract.TopicAssignmentPort;
import vn.io.ajarline.content.infrastructure.persistence.ArticleEntity;
import vn.io.ajarline.content.infrastructure.persistence.ArticleJpaRepository;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemEntity;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemJpaRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads and writes the common record of content items of any type.
 * It also applies the publication changes that the publication workflow asks for.
 * The caller owns the transaction. Methods that touch the database fail without one.
 */
@Service
public class ContentRecordService
        implements ContentRecordReader, ContentRecordWriter, ContentPublicationAccess, ContentEligibility {
    private static final String SLUG_CONSTRAINT = "uk_content_item_type_slug";

    private final ContentItemJpaRepository contentRepository;
    private final ArticleJpaRepository articleRepository;
    private final ContentRecordPolicy policy;
    private final ArticlePolicy articlePolicy;
    private final Clock clock;
    private final TopicAssignmentPort topicAssignment;

    public ContentRecordService(ContentItemJpaRepository contentRepository,
                                ArticleJpaRepository articleRepository,
                                ContentRecordPolicy policy,
                                ArticlePolicy articlePolicy,
                                Clock clock,
                                TopicAssignmentPort topicAssignment) {
        this.contentRepository = contentRepository;
        this.articleRepository = articleRepository;
        this.policy = policy;
        this.articlePolicy = articlePolicy;
        this.clock = clock;
        this.topicAssignment = topicAssignment;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ContentRecordView get(UUID id, ContentType type) {
        return toView(requireItem(id, type));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<UUID> topicIdsOf(UUID id) {
        return topicAssignment.topicIdsOf(id);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Map<UUID, ContentRecordSummary> summariesOf(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ContentRecordSummary> summaries = new HashMap<>();
        for (ContentItemEntity item : contentRepository.findAllById(ids)) {
            summaries.put(item.getId(), toSummary(item));
        }
        return summaries;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<UUID> idOf(ContentType type, String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        return contentRepository.findIdByTypeAndSlug(type, slug);
    }

    @Override
    public ContentRecordInput normalize(ContentRecordInput input) {
        if (input == null) {
            throw new ContentOperationException(
                    ContentFailureCode.VALIDATION_FAILED, "Content is required.");
        }
        policy.requireValidTitle(input.title());
        String slug = policy.normalizeSlug(input.slug());
        String summary = policy.normalizeSummary(input.summary());
        policy.requireDistinctTopicIds(input.topicIds());
        return new ContentRecordInput(input.title(), slug, summary, input.topicIds());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ContentRecordView create(ContentType type, ContentRecordInput input) {
        UUID id = UUID.randomUUID();
        ContentItemEntity item = ContentItemEntity.draft(
                id, type, input.slug(), input.title(), input.summary(), clock.instant());

        contentRepository.save(item);
        flushSlugConflicts();
        // The content row must exist before an assignment can point at it.
        topicAssignment.replaceAssignments(id, input.topicIds());
        return toView(item);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public EditableRecord lockForEdit(UUID id, ContentType type, long expectedRevision) {
        return new EditableRecord(toView(lockAtRevision(id, type, expectedRevision)));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public EditableRecord lockForEdit(
            UUID id, ContentType type, long expectedRevision, ContentRecordInput input) {
        ContentItemEntity item = lockAtRevision(id, type, expectedRevision);

        // The slug check comes before the summary check: a changed slug cannot be
        // fixed by filling in more fields, so the editor is told first.
        if (item.hasEverBeenPublished() && !item.getSlug().equals(input.slug())) {
            throw new ContentOperationException(
                    ContentFailureCode.SLUG_IMMUTABLE_AFTER_PUBLICATION,
                    "Slug cannot change after the first publication.");
        }
        if (item.getStatus() == PublicationStatus.PUBLISHED) {
            policy.requirePublishableSummary(input.summary());
        }
        return new EditableRecord(toView(item));
    }

    /**
     * Locks the row, then compares the revision. The row must not be loaded before this call,
     * or the comparison would use a stale copy.
     */
    private ContentItemEntity lockAtRevision(UUID id, ContentType type, long expectedRevision) {
        ContentItemEntity item = contentRepository.findByIdForUpdate(id)
                .filter(found -> found.getType() == type)
                .orElseThrow(() -> notFound(id, type));
        requireRevision(item, expectedRevision);
        return item;
    }

    /**
     * Same as above, for any type. The publication workflow knows the type only after the lock.
     */
    private ContentItemEntity lockAtRevision(UUID id, long expectedRevision) {
        ContentItemEntity item = contentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> notFound(id));
        requireRevision(item, expectedRevision);
        return item;
    }

    private void requireRevision(ContentItemEntity item, long expectedRevision) {
        if (item.getRevision() != expectedRevision) {
            throw new ContentOperationException(
                    ContentFailureCode.REVISION_CONFLICT,
                    "The revision does not match the expected revision.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockStructure(UUID id, ContentType type) {
        return contentRepository.findByIdForUpdate(id)
                .filter(item -> item.getType() == type)
                .isPresent();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ContentRecordView replace(
            EditableRecord record, ContentRecordInput input, boolean subtypeChanged) {
        UUID id = record.current().id();
        // lockForEdit already loaded and locked this row, so this does not read again.
        ContentItemEntity item = contentRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("The locked content item disappeared: " + id));

        boolean hasCommonChanged = !item.hasSameEditableContent(
                input.title(), input.slug(), input.summary());
        // The assignment is written here, before any decision to return early, so
        // that one failure rolls back the whole save.
        boolean hasTopicsChanged = topicAssignment.replaceAssignments(id, input.topicIds());
        if (!hasCommonChanged && !hasTopicsChanged && !subtypeChanged) {
            return toView(item);
        }

        item.replaceEditTableContent(input.title(), input.slug(), input.summary(), clock.instant());
        flushSlugConflicts();
        return toView(item);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ContentRecordView lock(UUID id, long expectedRevision) {
        return toView(lockAtRevision(id, expectedRevision));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ContentRecordView markPublished(UUID id, long expectedRevision, Instant changedAt) {
        ContentItemEntity item = lockAtRevision(id, expectedRevision);
        item.markPublished(changedAt);
        contentRepository.flush();
        return toView(item);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ContentRecordView markUnpublished(UUID id, long expectedRevision, Instant changedAt) {
        ContentItemEntity item = lockAtRevision(id, expectedRevision);
        item.markUnpublished(changedAt);
        contentRepository.flush();
        return toView(item);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void requirePublishable(UUID id) {
        ContentItemEntity item = contentRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        if (item.getType() == ContentType.ARTICLE) {
            ArticleEntity article = articleRepository.findById(id).orElseThrow(
                    () -> new IllegalStateException("Article subtype row is missing for " + id));
            articlePolicy.requirePublishable(item.getSummary(), article.getBodyMarkdown());
            return;
        }
        policy.requirePublishableSummary(item.getSummary());
    }

    private ContentItemEntity requireItem(UUID id, ContentType type) {
        return contentRepository.findById(id)
                .filter(item -> item.getType() == type)
                .orElseThrow(() -> notFound(id, type));
    }

    private ContentOperationException notFound(UUID id, ContentType type) {
        return new ContentOperationException(
                ContentFailureCode.CONTENT_NOT_FOUND,
                type + " was not found: " + id);
    }

    private ContentOperationException notFound(UUID id) {
        return new ContentOperationException(
                ContentFailureCode.CONTENT_NOT_FOUND,
                "Content was not found: " + id);
    }

    /**
     * Flushes pending changes, so a duplicate slug fails here. It turns that failure into a content failure.
     */
    private void flushSlugConflicts() {
        try {
            contentRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, SLUG_CONSTRAINT)) {
                throw new ContentOperationException(
                        ContentFailureCode.SLUG_CONFLICT,
                        "Content of this type with this slug already exists.",
                        exception);
            }
            throw exception;
        }
    }

    private boolean containsConstraint(Throwable failure, String constraint) {
        Throwable current = failure;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(constraint)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ContentRecordView toView(ContentItemEntity item) {
        return new ContentRecordView(
                item.getId(),
                item.getType(),
                item.getTitle(),
                item.getSlug(),
                item.getSummary(),
                item.getStatus(),
                item.getRevision(),
                item.getPublishedAt(),
                item.getUnpublishedAt(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    /**
     * Also used by the content index, so both reads show the same columns.
     */
    static ContentRecordSummary toSummary(ContentItemEntity item) {
        return new ContentRecordSummary(
                item.getId(),
                item.getType(),
                item.getTitle(),
                item.getSlug(),
                item.getSummary(),
                item.getStatus(),
                item.getRevision(),
                item.getPublishedAt(),
                item.getUpdatedAt());
    }
}
