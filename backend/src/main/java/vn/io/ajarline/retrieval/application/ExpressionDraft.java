package vn.io.ajarline.retrieval.application;

/**
 * Holds one expression that already passed the Concept policy.
 *
 * <p>The text is trimmed. The normalized text is computed once by
 * {@link ExpressionNormalizer}. The language hint is null or one allowed value.</p>
 */
public record ExpressionDraft(
        String expression,
        String normalizedExpression,
        String languageHint,
        boolean autoExpand
) {
}
