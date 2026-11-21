package vn.io.ajarline.retrieval.contract;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Holds a snapshot of one Concept returned by the retrieval capability.
 *
 * <p>The expressions are ordered by normalized text and then by text.
 * This is an immutable output object. It is not a database entity and it is
 * not an HTTP response.</p>
 */
public record ConceptView(
        UUID id,
        String key,
        String displayName,
        String description,
        boolean active,
        List<ConceptExpressionView> expressions,
        Instant createdAt,
        Instant updatedAt
) {
}
