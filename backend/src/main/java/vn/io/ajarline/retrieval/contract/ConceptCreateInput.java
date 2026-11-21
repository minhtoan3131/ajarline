package vn.io.ajarline.retrieval.contract;

import java.util.List;

/**
 * Carries the values needed to create a Concept.
 *
 * <p>The values are not validated here. The retrieval capability normalizes and
 * validates them before saving.</p>
 */
public record ConceptCreateInput(
        String key,
        String displayName,
        String description,
        boolean active,
        List<ConceptExpressionInput> expressions
) {
}
