package vn.io.ajarline.retrieval.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import vn.io.ajarline.retrieval.application.ExpressionDraft;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "retrieval_concept_expression")
public class ConceptExpressionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concept_id", nullable = false, updatable = false)
    private ConceptEntity concept;

    @Column(name = "expression", nullable = false, length = 300)
    private String expression;

    @Column(name = "normalized_expression", nullable = false, columnDefinition = "text")
    private String normalizedExpression;

    @Column(name = "language_hint", length = 16)
    private String languageHint;

    @Column(name = "auto_expand", nullable = false)
    private boolean autoExpand;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Required by JPA
     */
    protected ConceptExpressionEntity() {

    }

    private ConceptExpressionEntity(UUID id, ConceptEntity concept, ExpressionDraft draft, Instant now) {
        this.id = id;
        this.concept = concept;
        this.expression = draft.expression();
        this.normalizedExpression = draft.normalizedExpression();
        this.languageHint = draft.languageHint();
        this.autoExpand = draft.autoExpand();
        this.createdAt = now;
        this.updatedAt = now;
    }

    static ConceptExpressionEntity create(
            UUID id, ConceptEntity concept, ExpressionDraft draft, Instant now) {
        return new ConceptExpressionEntity(id, concept, draft, now);
    }

    boolean hasSameState(ExpressionDraft draft) {
        return Objects.equals(expression, draft.expression())
                && Objects.equals(languageHint, draft.languageHint())
                && autoExpand == draft.autoExpand();
    }

    /**
     * Copies the draft into this row. The time changes only when a value differs.
     */
    void replaceState(ExpressionDraft draft, Instant now) {
        if (hasSameState(draft)) {
            return;
        }
        this.expression = draft.expression();
        this.languageHint = draft.languageHint();
        this.autoExpand = draft.autoExpand();
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getExpression() {
        return expression;
    }

    public String getNormalizedExpression() {
        return normalizedExpression;
    }

    public String getLanguageHint() {
        return languageHint;
    }

    public boolean isAutoExpand() {
        return autoExpand;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
