package vn.io.ajarline.retrieval.application;

import org.springframework.stereotype.Component;
import vn.io.ajarline.retrieval.contract.ConceptCreateInput;
import vn.io.ajarline.retrieval.contract.ConceptExpressionInput;
import vn.io.ajarline.retrieval.contract.ConceptFailureCode;
import vn.io.ajarline.retrieval.contract.ConceptOperationException;
import vn.io.ajarline.retrieval.contract.ConceptUpdateInput;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Normalizes and validates Concept values.
 *
 * <p>The limits match the database column sizes. The database stays the last
 * line of defense; this policy gives a clear business failure first.</p>
 */
@Component
public class ConceptPolicy {
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z0-9]+(?:[-_.][a-z0-9]+)*$");
    private static final int KEY_MAX_LENGTH = 160;
    private static final int DISPLAY_NAME_MAX_LENGTH = 200;
    private static final int DESCRIPTION_MAX_LENGTH = 4000;
    private static final int EXPRESSION_MAX_COUNT = 50;
    private static final int EXPRESSION_MAX_LENGTH = 300;
    private static final Set<String> LANGUAGE_HINTS = Set.of("vi", "en", "technical", "mixed");

    /**
     * Returns the Concept that the create operation may save.
     *
     * <p>The key is trimmed and lowercased. An empty description becomes null.
     * Each expression is trimmed and gets its normalized text.</p>
     *
     * @throws ConceptOperationException when a value violates the limits, or when two
     *         expressions have the same normalized text
     */
    public NewConcept normalizeCreate(ConceptCreateInput input) {
        if (input == null) {
            throw failure("Concept content is required.");
        }
        String key = normalizeKey(input.key());
        validateText(key, "Key", KEY_MAX_LENGTH, false);
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw failure("Key must use lowercase letters, digits, and single separators - _ or .");
        }
        return new NewConcept(key, normalizeEditable(
                input.displayName(), input.description(), input.active(), input.expressions()));
    }

    /**
     * Returns the editable values that the update operation may save.
     *
     * @throws ConceptOperationException when a value violates the limits, or when two
     *         expressions have the same normalized text
     */
    public EditableConcept normalizeUpdate(ConceptUpdateInput input) {
        if (input == null) {
            throw failure("Concept content is required.");
        }
        return normalizeEditable(
                input.displayName(), input.description(), input.active(), input.expressions());
    }

    private EditableConcept normalizeEditable(
            String displayName, String description, boolean active,
            List<ConceptExpressionInput> expressions) {
        validateText(displayName, "Display name", DISPLAY_NAME_MAX_LENGTH, false);
        String normalizedDescription = normalizeDescription(description);
        validateText(normalizedDescription, "Description", DESCRIPTION_MAX_LENGTH, true);
        return new EditableConcept(
                displayName, normalizedDescription, active, normalizeExpressions(expressions));
    }

    private List<ExpressionDraft> normalizeExpressions(List<ConceptExpressionInput> expressions) {
        if (expressions == null) {
            throw failure("Expressions are required.");
        }
        if (expressions.size() > EXPRESSION_MAX_COUNT) {
            throw failure("A Concept must not have more than " + EXPRESSION_MAX_COUNT + " expressions.");
        }
        List<ExpressionDraft> drafts = new ArrayList<>();
        for (ConceptExpressionInput input : expressions) {
            drafts.add(toDraft(input));
        }
        rejectDuplicates(drafts);
        return drafts;
    }

    private ExpressionDraft toDraft(ConceptExpressionInput input) {
        if (input == null || input.expression() == null) {
            throw failure("Expression text is required.");
        }
        String expression = input.expression().strip();
        if (expression.codePointCount(0, expression.length()) > EXPRESSION_MAX_LENGTH) {
            throw failure("Expression text must not exceed " + EXPRESSION_MAX_LENGTH + " characters.");
        }
        String normalized = ExpressionNormalizer.normalize(expression);
        if (normalized.isEmpty()) {
            throw failure("Expression text must not be blank.");
        }
        String languageHint = input.languageHint();
        if (languageHint != null && !LANGUAGE_HINTS.contains(languageHint)) {
            throw failure("Language hint must be vi, en, technical or mixed.");
        }
        return new ExpressionDraft(expression, normalized, languageHint, input.autoExpand());
    }

    private void rejectDuplicates(List<ExpressionDraft> drafts) {
        Set<String> seen = new HashSet<>();
        for (ExpressionDraft draft : drafts) {
            if (!seen.add(draft.normalizedExpression())) {
                throw new ConceptOperationException(
                        ConceptFailureCode.DUPLICATE_CONCEPT_EXPRESSION,
                        "Two expressions are the same after normalization.");
            }
        }
    }

    private String normalizeKey(String key) {
        if (key == null) {
            throw failure("Key is required.");
        }
        return key.strip().toLowerCase(Locale.ROOT);
    }

    private String normalizeDescription(String description) {
        if (description != null && description.isEmpty()) {
            return null;
        }
        return description;
    }

    private void validateText(String value, String fieldName, int maximumLength, boolean nullable) {
        if (value == null) {
            if (nullable) {
                return;
            }
            throw failure(fieldName + " is required.");
        }
        if (value.isBlank()) {
            throw failure(fieldName + " must not be blank.");
        }
        int length = value.codePointCount(0, value.length());
        if (length > maximumLength) {
            throw failure(fieldName + " must not exceed " + maximumLength + " characters.");
        }
    }

    private ConceptOperationException failure(String message) {
        return new ConceptOperationException(ConceptFailureCode.VALIDATION_FAILED, message);
    }
}
