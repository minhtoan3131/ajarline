package vn.io.ajarline.topic.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentRevisionCommands;
import vn.io.ajarline.topic.contract.TopicCreateInput;
import vn.io.ajarline.topic.contract.TopicCreator;
import vn.io.ajarline.topic.contract.TopicEditor;
import vn.io.ajarline.topic.contract.TopicFailureCode;
import vn.io.ajarline.topic.contract.TopicOperationException;
import vn.io.ajarline.topic.contract.TopicReader;
import vn.io.ajarline.topic.contract.TopicRemover;
import vn.io.ajarline.topic.contract.TopicUpdateInput;
import vn.io.ajarline.topic.contract.TopicView;
import vn.io.ajarline.topic.infrastructure.persistence.TopicEntity;
import vn.io.ajarline.topic.infrastructure.persistence.TopicJpaRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Runs the Topic catalog use cases.
 *
 * <p>Each public method is one transaction. The database constraints are the
 * authority for slug uniqueness; this service only translates their failures
 * into Topic failure codes.</p>
 */
@Service
public class TopicService implements TopicCreator, TopicReader, TopicEditor, TopicRemover {
    private static final String SLUG_CONSTRAINT = "uk_topic_slug";
    private static final String ASSIGNMENT_CONSTRAINT = "fk_content_topic_topic_id";
    private static final Sort LIST_ORDER = Sort.by(
            Sort.Order.asc("name"),
            Sort.Order.asc("id"));

    private final TopicJpaRepository topics;
    private final TopicPolicy policy;
    private final Clock clock;
    private final ContentRevisionCommands contentRevisions;

    public TopicService(TopicJpaRepository topics, TopicPolicy policy, Clock clock,
                        ContentRevisionCommands contentRevisions) {
        this.topics = topics;
        this.policy = policy;
        this.clock = clock;
        this.contentRevisions = contentRevisions;
    }

    @Override
    @Transactional
    public TopicView create(TopicCreateInput input) {
        TopicCreateInput normalized = policy.normalizeCreate(input);
        TopicEntity topic = TopicEntity.create(
                UUID.randomUUID(),
                normalized.slug(),
                normalized.name(),
                normalized.description(),
                normalized.active(),
                clock.instant());

        try {
            topics.saveAndFlush(topic);
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, SLUG_CONSTRAINT)) {
                throw new TopicOperationException(
                        TopicFailureCode.SLUG_CONFLICT,
                        "A Topic with this slug already exists.",
                        exception);
            }
            throw exception;
        }
        return toView(topic);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicView> list() {
        return topics.findAll(LIST_ORDER).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional
    public TopicView replace(UUID id, TopicUpdateInput input) {
        TopicUpdateInput normalized = policy.normalizeUpdate(input);
        TopicEntity topic = topics.findByIdForUpdate(id)
                .orElseThrow(() -> notFound(id));

        if (topic.hasSameEditableState(
                normalized.name(), normalized.description(), normalized.active())) {
            return toView(topic);
        }

        boolean renamed = !topic.hasSameName(normalized.name());
        Instant now = clock.instant();
        topic.replaceEditableState(
                normalized.name(),
                normalized.description(),
                normalized.active(),
                now);
        if (renamed) {
            // The name shows with every content item that carries this Topic,
            // so those items get a new revision. Content owns the revision.
            contentRevisions.advanceRevisions(topics.findAssignedContentIds(id), now);
        }
        return toView(topic);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        TopicEntity topic = topics.findByIdForUpdate(id)
                .orElseThrow(() -> notFound(id));
        if (topics.isAssigned(id)) {
            throw inUse(null);
        }

        try {
            topics.delete(topic);
            topics.flush();
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, ASSIGNMENT_CONSTRAINT)) {
                throw inUse(exception);
            }
            throw exception;
        }
    }

    private TopicOperationException inUse(Throwable cause) {
        return new TopicOperationException(
                TopicFailureCode.TOPIC_IN_USE,
                "The Topic is still assigned to content.",
                cause);
    }

    private TopicOperationException notFound(UUID id) {
        return new TopicOperationException(
                TopicFailureCode.TOPIC_NOT_FOUND,
                "Topic was not found: " + id);
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

    private TopicView toView(TopicEntity topic) {
        return new TopicView(
                topic.getId(),
                topic.getSlug(),
                topic.getName(),
                topic.getDescription(),
                topic.isActive(),
                topic.getCreatedAt(),
                topic.getUpdatedAt());
    }
}
