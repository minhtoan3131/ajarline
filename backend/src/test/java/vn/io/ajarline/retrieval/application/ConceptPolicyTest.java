package vn.io.ajarline.retrieval.application;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.io.ajarline.retrieval.contract.ConceptCreateInput;
import vn.io.ajarline.retrieval.contract.ConceptExpressionInput;
import vn.io.ajarline.retrieval.contract.ConceptFailureCode;
import vn.io.ajarline.retrieval.contract.ConceptOperationException;
import vn.io.ajarline.retrieval.contract.ConceptUpdateInput;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConceptPolicyTest {

    private final ConceptPolicy policy = new ConceptPolicy();

    @Test
    void normalizeCreate_ShouldTrimAndLowercaseKey_WhenKeyHasSpacesAndCapitals() {
        NewConcept result = policy.normalizeCreate(new ConceptCreateInput(
                "  Multi-Threading ", "Threading", "Many tasks.", true, List.of()));

        assertThat(result.key()).isEqualTo("multi-threading");
        assertThat(result.editable().displayName()).isEqualTo("Threading");
        assertThat(result.editable().description()).isEqualTo("Many tasks.");
        assertThat(result.editable().active()).isTrue();
    }

    @ParameterizedTest(name = "[{index}] accepts key \"{0}\"")
    @ValueSource(strings = {"thread", "multi-threading", "java.util.concurrent", "a_b", "a1-b2_c3.d4"})
    void normalizeCreate_ShouldAcceptKey_WhenFormatIsValid(String key) {
        NewConcept result = policy.normalizeCreate(withKey(key));

        assertThat(result.key()).isEqualTo(key);
    }

    @ParameterizedTest(name = "[{index}] rejects key \"{0}\"")
    @ValueSource(strings = {"", "   ", "_a", "a_", "a__b", "a b", "a/b", "a.-b", "-a", "a--b"})
    void normalizeCreate_ShouldRejectKey_WhenFormatIsInvalid(String key) {
        assertValidationFailure(() -> policy.normalizeCreate(withKey(key)));
    }

    @Test
    void normalizeCreate_ShouldRejectKey_WhenKeyIsMissing() {
        assertValidationFailure(() -> policy.normalizeCreate(withKey(null)));
    }

    @Test
    void normalizeCreate_ShouldAcceptKey_WhenKeyHasExactlyMaximumLength() {
        String key = "a".repeat(160);

        assertThat(policy.normalizeCreate(withKey(key)).key()).isEqualTo(key);
    }

    @Test
    void normalizeCreate_ShouldRejectKey_WhenKeyExceedsMaximumLength() {
        assertValidationFailure(() -> policy.normalizeCreate(withKey("a".repeat(161))));
    }

    @Test
    void normalizeCreate_ShouldRejectDisplayName_WhenNameIsMissingOrBlank() {
        assertValidationFailure(() -> policy.normalizeCreate(
                new ConceptCreateInput("thread", null, null, true, List.of())));
        assertValidationFailure(() -> policy.normalizeCreate(
                new ConceptCreateInput("thread", "   ", null, true, List.of())));
    }

    @Test
    void normalizeCreate_ShouldCheckDisplayNameLength_WhenNameIsAtAndOverTheLimit() {
        String atLimit = "a".repeat(200);

        assertThat(policy.normalizeCreate(new ConceptCreateInput(
                "thread", atLimit, null, true, List.of())).editable().displayName()).isEqualTo(atLimit);
        assertValidationFailure(() -> policy.normalizeCreate(new ConceptCreateInput(
                "thread", "a".repeat(201), null, true, List.of())));
    }

    @Test
    void normalizeCreate_ShouldStoreNullDescription_WhenDescriptionIsEmpty() {
        NewConcept result = policy.normalizeCreate(
                new ConceptCreateInput("thread", "Thread", "", true, List.of()));

        assertThat(result.editable().description()).isNull();
    }

    @Test
    void normalizeCreate_ShouldCheckDescriptionLength_WhenDescriptionIsAtAndOverTheLimit() {
        assertThat(policy.normalizeCreate(new ConceptCreateInput(
                "thread", "Thread", "a".repeat(4000), true, List.of())).editable().description())
                .hasSize(4000);
        assertValidationFailure(() -> policy.normalizeCreate(new ConceptCreateInput(
                "thread", "Thread", "a".repeat(4001), true, List.of())));
    }

    @Test
    void normalizeCreate_ShouldRejectConcept_WhenExpressionListIsMissing() {
        assertValidationFailure(() -> policy.normalizeCreate(
                new ConceptCreateInput("thread", "Thread", null, true, null)));
    }

    @Test
    void normalizeCreate_ShouldCheckExpressionCount_WhenCountIsAtAndOverTheLimit() {
        assertThat(policy.normalizeCreate(withExpressions(manyExpressions(50))).editable().expressions())
                .hasSize(50);
        assertValidationFailure(() -> policy.normalizeCreate(withExpressions(manyExpressions(51))));
    }

    @Test
    void normalizeCreate_ShouldTrimExpressionAndComputeNormalizedText_WhenExpressionIsValid() {
        NewConcept result = policy.normalizeCreate(withExpressions(
                new ConceptExpressionInput("  Spring  Boot ", "en", true)));

        ExpressionDraft draft = result.editable().expressions().get(0);
        assertThat(draft.expression()).isEqualTo("Spring  Boot");
        assertThat(draft.normalizedExpression()).isEqualTo("spring boot");
        assertThat(draft.languageHint()).isEqualTo("en");
        assertThat(draft.autoExpand()).isTrue();
    }

    @Test
    void normalizeCreate_ShouldRejectExpression_WhenTextIsMissingOrBlank() {
        assertValidationFailure(() -> policy.normalizeCreate(withExpressions(
                new ConceptExpressionInput(null, "en", true))));
        assertValidationFailure(() -> policy.normalizeCreate(withExpressions(
                new ConceptExpressionInput("   ", "en", true))));
        assertValidationFailure(() -> policy.normalizeCreate(withExpressions(
                new ConceptExpressionInput(" ", "en", true))));
    }

    @Test
    void normalizeCreate_ShouldCheckExpressionLength_WhenTextIsAtAndOverTheLimit() {
        assertThat(policy.normalizeCreate(withExpressions(
                new ConceptExpressionInput("a".repeat(300), null, true))).editable().expressions())
                .hasSize(1);
        assertValidationFailure(() -> policy.normalizeCreate(withExpressions(
                new ConceptExpressionInput("a".repeat(301), null, true))));
    }

    @ParameterizedTest(name = "[{index}] accepts language hint {0}")
    @ValueSource(strings = {"vi", "en", "technical", "mixed"})
    void normalizeCreate_ShouldAcceptLanguageHint_WhenHintIsAllowed(String hint) {
        NewConcept result = policy.normalizeCreate(withExpressions(
                new ConceptExpressionInput("thread", hint, true)));

        assertThat(result.editable().expressions().get(0).languageHint()).isEqualTo(hint);
    }

    @Test
    void normalizeCreate_ShouldAcceptNullLanguageHint_WhenHintIsNotGiven() {
        NewConcept result = policy.normalizeCreate(withExpressions(
                new ConceptExpressionInput("thread", null, true)));

        assertThat(result.editable().expressions().get(0).languageHint()).isNull();
    }

    @ParameterizedTest(name = "[{index}] rejects language hint \"{0}\"")
    @ValueSource(strings = {"fr", "EN", "", " vi"})
    void normalizeCreate_ShouldRejectLanguageHint_WhenHintIsNotAllowed(String hint) {
        assertValidationFailure(() -> policy.normalizeCreate(withExpressions(
                new ConceptExpressionInput("thread", hint, true))));
    }

    @Test
    void normalizeCreate_ShouldRejectDuplicate_WhenExpressionsAreTheSameAfterNormalization() {
        ThrowingCallable call = () -> policy.normalizeCreate(withExpressions(
                new ConceptExpressionInput("Thread", "en", true),
                new ConceptExpressionInput(" thread ", "vi", false)));

        assertThatThrownBy(call)
                .isInstanceOfSatisfying(ConceptOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ConceptFailureCode.DUPLICATE_CONCEPT_EXPRESSION));
    }

    @Test
    void normalizeCreate_ShouldAcceptExpressions_WhenNormalizedTextsDiffer() {
        NewConcept result = policy.normalizeCreate(withExpressions(
                new ConceptExpressionInput("Thread", "en", true),
                new ConceptExpressionInput("Threads", "en", true)));

        assertThat(result.editable().expressions()).hasSize(2);
    }

    @Test
    void normalizeCreate_ShouldReportValidationFailure_WhenInputHasBothDuplicateAndInvalidHint() {
        assertValidationFailure(() -> policy.normalizeCreate(withExpressions(
                new ConceptExpressionInput("Thread", "en", true),
                new ConceptExpressionInput("thread", "fr", true))));
    }

    @Test
    void normalizeCreate_ShouldRejectInput_WhenInputIsMissing() {
        assertValidationFailure(() -> policy.normalizeCreate(null));
    }

    @Test
    void normalizeUpdate_ShouldReturnEditableValues_WhenInputIsValid() {
        EditableConcept result = policy.normalizeUpdate(new ConceptUpdateInput(
                "Threading", "", false,
                List.of(new ConceptExpressionInput(" Thread ", "en", true))));

        assertThat(result.displayName()).isEqualTo("Threading");
        assertThat(result.description()).isNull();
        assertThat(result.active()).isFalse();
        assertThat(result.expressions()).hasSize(1);
        assertThat(result.expressions().get(0).normalizedExpression()).isEqualTo("thread");
    }

    @Test
    void normalizeUpdate_ShouldRejectDisplayName_WhenNameIsBlank() {
        assertValidationFailure(() -> policy.normalizeUpdate(
                new ConceptUpdateInput("   ", null, true, List.of())));
    }

    @Test
    void normalizeUpdate_ShouldRejectMoreThanFiftyExpressions_WhenSetIsTooBig() {
        assertValidationFailure(() -> policy.normalizeUpdate(
                new ConceptUpdateInput("Thread", null, true, manyExpressions(51))));
    }

    @Test
    void normalizeUpdate_ShouldRejectDuplicate_WhenExpressionsAreTheSameAfterNormalization() {
        assertThatThrownBy(() -> policy.normalizeUpdate(new ConceptUpdateInput(
                "Thread", null, true,
                List.of(new ConceptExpressionInput("Thread", "en", true),
                        new ConceptExpressionInput("THREAD", "en", true)))))
                .isInstanceOfSatisfying(ConceptOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ConceptFailureCode.DUPLICATE_CONCEPT_EXPRESSION));
    }

    @Test
    void normalizeUpdate_ShouldRejectInput_WhenInputIsMissing() {
        assertValidationFailure(() -> policy.normalizeUpdate(null));
    }

    private static ConceptCreateInput withKey(String key) {
        return new ConceptCreateInput(key, "Name", null, true, List.of());
    }

    private static ConceptCreateInput withExpressions(ConceptExpressionInput... expressions) {
        return withExpressions(List.of(expressions));
    }

    private static ConceptCreateInput withExpressions(List<ConceptExpressionInput> expressions) {
        return new ConceptCreateInput("thread", "Thread", null, true, expressions);
    }

    private static List<ConceptExpressionInput> manyExpressions(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> new ConceptExpressionInput("expression " + index, "en", true))
                .toList();
    }

    private static void assertValidationFailure(ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(ConceptOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ConceptFailureCode.VALIDATION_FAILED));
    }
}
