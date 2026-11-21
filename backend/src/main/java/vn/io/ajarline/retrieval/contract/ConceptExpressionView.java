package vn.io.ajarline.retrieval.contract;

/**
 * Holds one expression returned by the retrieval capability.
 */
public record ConceptExpressionView(
        String expression,
        String languageHint,
        boolean autoExpand
) {
}
