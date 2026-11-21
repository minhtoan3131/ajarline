package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.retrieval.contract.ConceptView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents one Concept returned by the Admin API, including inactive Concepts.
 */
@Schema(requiredProperties = {
        "id", "key", "displayName", "description", "active", "expressions", "createdAt", "updatedAt"
})
public record ConceptAdminItem(
        UUID id,
        String key,
        String displayName,
        String description,
        boolean active,
        List<ConceptExpressionItem> expressions,
        Instant createdAt,
        Instant updatedAt
) {
    static ConceptAdminItem from(ConceptView view) {
        return new ConceptAdminItem(
                view.id(),
                view.key(),
                view.displayName(),
                view.description(),
                view.active(),
                view.expressions().stream().map(ConceptExpressionItem::from).toList(),
                view.createdAt(),
                view.updatedAt());
    }
}
