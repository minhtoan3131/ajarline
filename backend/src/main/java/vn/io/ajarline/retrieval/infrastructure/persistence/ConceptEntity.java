package vn.io.ajarline.retrieval.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import vn.io.ajarline.retrieval.application.EditableConcept;
import vn.io.ajarline.retrieval.application.ExpressionDraft;
import vn.io.ajarline.retrieval.application.NewConcept;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Stores one Concept and owns the set of its expressions.
 *
 * <p>The set is matched by normalized text. A kept expression is changed in place,
 * a new one is added and a missing one is removed. The database never sees two rows
 * with the same normalized text, even for a moment.</p>
 */
@Entity
@Table(name = "retrieval_concept")
public class ConceptEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "key", nullable = false, updatable = false, length = 160)
    private String key;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "concept", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConceptExpressionEntity> expressions = new ArrayList<>();

    /**
     * Required by JPA
     */
    protected ConceptEntity() {

    }

    private ConceptEntity(UUID id, String key, EditableConcept editable, Instant now) {
        this.id = id;
        this.key = key;
        this.displayName = editable.displayName();
        this.description = editable.description();
        this.active = editable.active();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Creates a Concept with its expressions. The caller gives the source of expression ids.
     */
    public static ConceptEntity create(
            UUID id, NewConcept concept, Instant now, Supplier<UUID> expressionIds) {
        ConceptEntity entity = new ConceptEntity(id, concept.key(), concept.editable(), now);
        for (ExpressionDraft draft : concept.editable().expressions()) {
            entity.expressions.add(
                    ConceptExpressionEntity.create(expressionIds.get(), entity, draft, now));
        }
        return entity;
    }

    public boolean hasSameEditableState(EditableConcept editable) {
        return Objects.equals(displayName, editable.displayName())
                && Objects.equals(description, editable.description())
                && active == editable.active()
                && hasSameExpressions(editable.expressions());
    }

    /**
     * Replaces the editable values and the whole expression set.
     */
    public void replaceEditableState(
            EditableConcept editable, Instant now, Supplier<UUID> expressionIds) {
        this.displayName = editable.displayName();
        this.description = editable.description();
        this.active = editable.active();
        this.updatedAt = now;

        Map<String, ConceptExpressionEntity> stored = expressionsByNormalizedText();
        Set<String> kept = new HashSet<>();
        for (ExpressionDraft draft : editable.expressions()) {
            ConceptExpressionEntity existing = stored.get(draft.normalizedExpression());
            if (existing == null) {
                expressions.add(
                        ConceptExpressionEntity.create(expressionIds.get(), this, draft, now));
            } else {
                existing.replaceState(draft, now);
            }
            kept.add(draft.normalizedExpression());
        }
        expressions.removeIf(expression -> !kept.contains(expression.getNormalizedExpression()));
    }

    private boolean hasSameExpressions(List<ExpressionDraft> drafts) {
        if (expressions.size() != drafts.size()) {
            return false;
        }
        Map<String, ConceptExpressionEntity> stored = expressionsByNormalizedText();
        return drafts.stream().allMatch(draft -> {
            ConceptExpressionEntity existing = stored.get(draft.normalizedExpression());
            return existing != null && existing.hasSameState(draft);
        });
    }

    private Map<String, ConceptExpressionEntity> expressionsByNormalizedText() {
        return expressions.stream().collect(Collectors.toMap(
                ConceptExpressionEntity::getNormalizedExpression, Function.identity()));
    }

    public UUID getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ConceptExpressionEntity> getExpressions() {
        return Collections.unmodifiableList(expressions);
    }
}
