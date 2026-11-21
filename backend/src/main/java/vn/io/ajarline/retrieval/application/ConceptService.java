package vn.io.ajarline.retrieval.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.retrieval.contract.ConceptCreateInput;
import vn.io.ajarline.retrieval.contract.ConceptCreator;
import vn.io.ajarline.retrieval.contract.ConceptEditor;
import vn.io.ajarline.retrieval.contract.ConceptExpressionView;
import vn.io.ajarline.retrieval.contract.ConceptFailureCode;
import vn.io.ajarline.retrieval.contract.ConceptOperationException;
import vn.io.ajarline.retrieval.contract.ConceptReader;
import vn.io.ajarline.retrieval.contract.ConceptRemover;
import vn.io.ajarline.retrieval.contract.ConceptUpdateInput;
import vn.io.ajarline.retrieval.contract.ConceptView;
import vn.io.ajarline.retrieval.infrastructure.persistence.ConceptEntity;
import vn.io.ajarline.retrieval.infrastructure.persistence.ConceptExpressionEntity;
import vn.io.ajarline.retrieval.infrastructure.persistence.ConceptJpaRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Runs the Concept vocabulary use cases.
 *
 * <p>Each public method is one transaction. A change raises the vocabulary version
 * as its last step, so the version row is locked for the shortest time. The database
 * constraint is the authority for key uniqueness; this service only translates its failure.</p>
 */
@Service
public class ConceptService
        implements ConceptCreator, ConceptReader, ConceptEditor, ConceptRemover {
    private static final String KEY_CONSTRAINT = "uk_retrieval_concept_key";
    private static final Comparator<ConceptExpressionEntity> EXPRESSION_ORDER =
            Comparator.comparing(ConceptExpressionEntity::getNormalizedExpression)
                    .thenComparing(ConceptExpressionEntity::getExpression);

    private final ConceptJpaRepository concepts;
    private final ConceptPolicy policy;
    private final VocabularyVersion vocabularyVersion;
    private final Clock clock;

    public ConceptService(ConceptJpaRepository concepts, ConceptPolicy policy,
                          VocabularyVersion vocabularyVersion, Clock clock) {
        this.concepts = concepts;
        this.policy = policy;
        this.vocabularyVersion = vocabularyVersion;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ConceptView create(ConceptCreateInput input) {
        NewConcept concept = policy.normalizeCreate(input);
        Instant now = clock.instant();
        ConceptEntity entity = ConceptEntity.create(
                UUID.randomUUID(), concept, now, UUID::randomUUID);

        ConceptEntity saved;
        try {
            saved = concepts.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, KEY_CONSTRAINT)) {
                throw new ConceptOperationException(
                        ConceptFailureCode.CONCEPT_KEY_CONFLICT,
                        "A Concept with this key already exists.",
                        exception);
            }
            throw exception;
        }
        vocabularyVersion.advance(now);
        return toView(saved);
    }

    @Override
    @Transactional
    public ConceptView replace(UUID id, ConceptUpdateInput input) {
        EditableConcept editable = policy.normalizeUpdate(input);
        ConceptEntity concept = concepts.findByIdForUpdate(id)
                .orElseThrow(() -> notFound(id));

        if (concept.hasSameEditableState(editable)) {
            return toView(concept);
        }

        Instant now = clock.instant();
        concept.replaceEditableState(editable, now, UUID::randomUUID);
        // Write the changes first. Then the version row is locked only for the last step.
        concepts.flush();
        vocabularyVersion.advance(now);
        return toView(concept);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ConceptEntity concept = concepts.findByIdForUpdate(id)
                .orElseThrow(() -> notFound(id));

        concepts.delete(concept);
        concepts.flush();
        vocabularyVersion.advance(clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConceptView> list() {
        return concepts.findAllWithExpressions().stream()
                .map(this::toView)
                .toList();
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

    private ConceptOperationException notFound(UUID id) {
        return new ConceptOperationException(
                ConceptFailureCode.CONCEPT_NOT_FOUND,
                "Concept was not found: " + id);
    }

    private ConceptView toView(ConceptEntity concept) {
        List<ConceptExpressionView> expressions = concept.getExpressions().stream()
                .sorted(EXPRESSION_ORDER)
                .map(expression -> new ConceptExpressionView(
                        expression.getExpression(),
                        expression.getLanguageHint(),
                        expression.isAutoExpand()))
                .toList();
        return new ConceptView(
                concept.getId(),
                concept.getKey(),
                concept.getDisplayName(),
                concept.getDescription(),
                concept.isActive(),
                expressions,
                concept.getCreatedAt(),
                concept.getUpdatedAt());
    }
}
