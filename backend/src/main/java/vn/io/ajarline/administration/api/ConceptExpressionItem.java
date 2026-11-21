package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.retrieval.contract.ConceptExpressionView;

/**
 * Represents one expression returned by the Admin API.
 */
@Schema(requiredProperties = {"expression", "languageHint", "autoExpand"})
public record ConceptExpressionItem(
        String expression,
        String languageHint,
        boolean autoExpand
) {
    static ConceptExpressionItem from(ConceptExpressionView view) {
        return new ConceptExpressionItem(view.expression(), view.languageHint(), view.autoExpand());
    }
}
