package vn.io.ajarline.retrieval.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.retrieval.contract.ConceptCreateInput;
import vn.io.ajarline.retrieval.contract.ConceptCreator;
import vn.io.ajarline.retrieval.contract.ConceptEditor;
import vn.io.ajarline.retrieval.contract.ConceptExpressionInput;
import vn.io.ajarline.retrieval.contract.ConceptExpressionView;
import vn.io.ajarline.retrieval.contract.ConceptFailureCode;
import vn.io.ajarline.retrieval.contract.ConceptOperationException;
import vn.io.ajarline.retrieval.contract.ConceptUpdateInput;
import vn.io.ajarline.retrieval.contract.ConceptView;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.MutableClock;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.io.ajarline.support.RetrievalTestData.currentVersion;
import static vn.io.ajarline.support.RetrievalTestData.deleteAllConcepts;
import static vn.io.ajarline.support.RetrievalTestData.uuid;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class ConceptReplaceExpressionsIT {

    private static final Instant CREATED_AT = Instant.parse("2026-11-21T00:00:00Z");
    private static final Instant CHANGED_AT = Instant.parse("2026-11-22T00:00:00Z");

    @Autowired
    private ConceptCreator creator;

    @Autowired
    private ConceptEditor editor;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabaseAndClock() {
        deleteAllConcepts(jdbcTemplate);
        clock.set(CREATED_AT);
    }

    @Test
    void replace_ShouldChangeRowInPlace_WhenOnlyLetterCaseChanges() {
        ConceptView created = createConcept(expression("Thread", "en", true));
        clock.set(CHANGED_AT);

        ConceptView replaced = editor.replace(created.id(),
                update("Threading", true, expression("thread", "en", true)));

        assertThat(replaced.expressions()).extracting(ConceptExpressionView::expression)
                .containsExactly("thread");
        assertThat(expressionCount(created.id())).isEqualTo(1);
        assertThat(timestampOf("created_at", created.id())).isEqualTo(CREATED_AT);
        assertThat(timestampOf("updated_at", created.id())).isEqualTo(CHANGED_AT);
    }

    @Test
    void replace_ShouldReturnExactlyTheSentSet_WhenExpressionsAreAddedRemovedAndChanged() {
        ConceptView created = createConcept(
                expression("Alpha", "en", true), expression("Beta", "en", true), expression("Gamma", "en", true));
        clock.set(CHANGED_AT);

        ConceptView replaced = editor.replace(created.id(), update("Threading", true,
                expression("Beta", "en", true), expression("Gamma", "en", false), expression("Delta", "vi", true)));

        assertThat(replaced.expressions()).extracting(ConceptExpressionView::expression)
                .containsExactly("Beta", "Delta", "Gamma");
        assertThat(replaced.expressions()).extracting(ConceptExpressionView::autoExpand)
                .containsExactly(true, true, false);
        assertThat(expressionCount(created.id())).isEqualTo(3);
    }

    @Test
    void replace_ShouldRemoveEveryExpression_WhenSetIsEmpty() {
        ConceptView created = createConcept(expression("Alpha", "en", true), expression("Beta", "en", true));

        ConceptView replaced = editor.replace(created.id(), update("Threading", true));

        assertThat(replaced.expressions()).isEmpty();
        assertThat(expressionCount(created.id())).isZero();
    }

    @Test
    void replace_ShouldAcceptFiftyExpressions_WhenSetHasFifty() {
        ConceptView created = createConcept();

        ConceptView replaced = editor.replace(created.id(),
                update("Threading", true, manyExpressions(50)));

        assertThat(replaced.expressions()).hasSize(50);
    }

    @Test
    void replace_ShouldRejectFiftyOneExpressions_AndKeepState() {
        ConceptView created = createConcept(expression("Thread", "en", true));
        long before = currentVersion(jdbcTemplate);

        assertThatThrownBy(() -> editor.replace(created.id(),
                update("Threading", true, manyExpressions(51))))
                .isInstanceOfSatisfying(ConceptOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ConceptFailureCode.VALIDATION_FAILED));

        assertThat(expressionCount(created.id())).isEqualTo(1);
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void replace_ShouldKeepEverything_WhenNothingDiffers() {
        ConceptView created = createConcept(expression("Thread", "en", true), expression("Task", "en", false));
        long before = currentVersion(jdbcTemplate);
        clock.set(CHANGED_AT);

        ConceptView replaced = editor.replace(created.id(), update("Threading", true,
                expression("Thread", "en", true), expression("Task", "en", false)));

        assertThat(replaced).isEqualTo(created);
        assertThat(timestampOf("updated_at", created.id())).isEqualTo(CREATED_AT);
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void replace_ShouldKeepEverything_WhenExpressionsComeInAnotherOrder() {
        ConceptView created = createConcept(expression("Thread", "en", true), expression("Task", "en", false));
        long before = currentVersion(jdbcTemplate);
        clock.set(CHANGED_AT);

        ConceptView replaced = editor.replace(created.id(), update("Threading", true,
                expression("Task", "en", false), expression("Thread", "en", true)));

        assertThat(replaced).isEqualTo(created);
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void replace_ShouldRaiseVersionByOne_WhenOnlyAutoExpandChanges() {
        ConceptView created = createConcept(expression("Thread", "en", true));
        long before = currentVersion(jdbcTemplate);

        ConceptView replaced = editor.replace(created.id(),
                update("Threading", true, expression("Thread", "en", false)));

        assertThat(replaced.expressions().get(0).autoExpand()).isFalse();
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before + 1);
    }

    @Test
    void replace_ShouldRaiseVersionByOneAndChangeTime_WhenDisplayNameChanges() {
        ConceptView created = createConcept(expression("Thread", "en", true));
        long before = currentVersion(jdbcTemplate);
        clock.set(CHANGED_AT);

        ConceptView replaced = editor.replace(created.id(),
                update("Concurrency", true, expression("Thread", "en", true)));

        assertThat(replaced.displayName()).isEqualTo("Concurrency");
        assertThat(replaced.updatedAt()).isEqualTo(CHANGED_AT);
        assertThat(replaced.createdAt()).isEqualTo(CREATED_AT);
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before + 1);
    }

    @Test
    void replace_ShouldRaiseVersionByOne_WhenActiveChanges() {
        ConceptView created = createConcept(expression("Thread", "en", true));
        long before = currentVersion(jdbcTemplate);

        ConceptView replaced = editor.replace(created.id(),
                update("Threading", false, expression("Thread", "en", true)));

        assertThat(replaced.active()).isFalse();
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before + 1);
    }

    @Test
    void replace_ShouldKeepStateAndVersion_WhenExpressionsRepeatAfterNormalization() {
        ConceptView created = createConcept(expression("Thread", "en", true));
        long before = currentVersion(jdbcTemplate);

        assertThatThrownBy(() -> editor.replace(created.id(), update("Changed", true,
                expression("Task", "en", true), expression(" task ", "en", true))))
                .isInstanceOfSatisfying(ConceptOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ConceptFailureCode.DUPLICATE_CONCEPT_EXPRESSION));

        assertThat(storedDisplayName(created.id())).isEqualTo("Threading");
        assertThat(expressionCount(created.id())).isEqualTo(1);
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void replace_ShouldReportValidationFailure_WhenIdIsUnknownAndInputIsInvalid() {
        assertThatThrownBy(() -> editor.replace(uuid(404), update("   ", true)))
                .isInstanceOfSatisfying(ConceptOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ConceptFailureCode.VALIDATION_FAILED));
    }

    @Test
    void replace_ShouldReportNotFound_WhenIdIsUnknown() {
        long before = currentVersion(jdbcTemplate);

        assertThatThrownBy(() -> editor.replace(uuid(404), update("Threading", true)))
                .isInstanceOfSatisfying(ConceptOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ConceptFailureCode.CONCEPT_NOT_FOUND));

        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    private ConceptView createConcept(ConceptExpressionInput... expressions) {
        return creator.create(new ConceptCreateInput(
                "threading", "Threading", null, true, List.of(expressions)));
    }

    private static ConceptUpdateInput update(
            String displayName, boolean active, ConceptExpressionInput... expressions) {
        return new ConceptUpdateInput(displayName, null, active, List.of(expressions));
    }

    private static ConceptExpressionInput expression(String text, String languageHint, boolean autoExpand) {
        return new ConceptExpressionInput(text, languageHint, autoExpand);
    }

    private static ConceptExpressionInput[] manyExpressions(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> expression("expression " + index, "en", true))
                .toArray(ConceptExpressionInput[]::new);
    }

    private long expressionCount(UUID conceptId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM retrieval_concept_expression WHERE concept_id = ?",
                Long.class, conceptId);
    }

    private String storedDisplayName(UUID conceptId) {
        return jdbcTemplate.queryForObject(
                "SELECT display_name FROM retrieval_concept WHERE id = ?", String.class, conceptId);
    }

    private Instant timestampOf(String column, UUID conceptId) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM retrieval_concept WHERE id = ?",
                OffsetDateTime.class, conceptId).toInstant();
    }
}
