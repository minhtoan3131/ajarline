package vn.io.ajarline.retrieval.contract;

/**
 * Carries one expression as the Admin typed it. The values are not validated here.
 */
public record ConceptExpressionInput(
        String expression,
        String languageHint,
        boolean autoExpand
) {
}
