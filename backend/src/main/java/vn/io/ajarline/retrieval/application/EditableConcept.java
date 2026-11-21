package vn.io.ajarline.retrieval.application;

import java.util.List;

/**
 * Holds the editable values of a Concept after the policy accepted them.
 *
 * <p>The expressions are the complete set. No two of them have the same normalized text.</p>
 */
public record EditableConcept(
        String displayName,
        String description,
        boolean active,
        List<ExpressionDraft> expressions
) {

    public EditableConcept {
        expressions = List.copyOf(expressions);
    }
}
