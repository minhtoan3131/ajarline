package vn.io.ajarline.retrieval.contract;

import java.util.List;

/**
 * Carries the editable values of an existing Concept. The key is not part of it
 * because a Concept key never changes. The expressions are the complete new set.
 */
public record ConceptUpdateInput(
        String displayName,
        String description,
        boolean active,
        List<ConceptExpressionInput> expressions
) {
}
