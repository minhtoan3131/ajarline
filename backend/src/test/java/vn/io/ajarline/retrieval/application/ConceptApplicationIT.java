package vn.io.ajarline.retrieval.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.retrieval.contract.ConceptCreateInput;
import vn.io.ajarline.retrieval.contract.ConceptCreator;
import vn.io.ajarline.retrieval.contract.ConceptExpressionInput;
import vn.io.ajarline.retrieval.contract.ConceptExpressionView;
import vn.io.ajarline.retrieval.contract.ConceptFailureCode;
import vn.io.ajarline.retrieval.contract.ConceptOperationException;
import vn.io.ajarline.retrieval.contract.ConceptReader;
import vn.io.ajarline.retrieval.contract.ConceptView;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.MutableClock;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.io.ajarline.support.RetrievalTestData.currentVersion;
import static vn.io.ajarline.support.RetrievalTestData.deleteAllConcepts;
import static vn.io.ajarline.support.RetrievalTestData.insertConcept;
import static vn.io.ajarline.support.RetrievalTestData.uuid;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class ConceptApplicationIT {

    private static final Instant CREATED_AT = Instant.parse("2026-11-21T00:00:00Z");

    @Autowired
    private ConceptCreator creator;

    @Autowired
    private ConceptReader reader;

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
    void create_ShouldReturnConcept_WhenInputIsValid() {
        ConceptView created = creator.create(new ConceptCreateInput(
                " Threading ", "Threading", "Many tasks at once.", true,
                List.of(expression("Thread", "en", true), expression("Multithreading", "en", false))));

        assertThat(created.id()).isNotNull();
        assertThat(created.key()).isEqualTo("threading");
        assertThat(created.displayName()).isEqualTo("Threading");
        assertThat(created.description()).isEqualTo("Many tasks at once.");
        assertThat(created.active()).isTrue();
        assertThat(created.createdAt()).isEqualTo(CREATED_AT);
        assertThat(created.updatedAt()).isEqualTo(CREATED_AT);
        assertThat(created.expressions()).extracting(ConceptExpressionView::expression)
                .containsExactly("Multithreading", "Thread");
        assertThat(conceptCount()).isEqualTo(1);
    }

    @Test
    void create_ShouldStoreNullDescription_WhenDescriptionIsEmpty() {
        ConceptView created = creator.create(input("threading", "Threading"));

        assertThat(created.description()).isNull();
        assertThat(created.expressions()).isEmpty();
    }

    @Test
    void create_ShouldRaiseVersionByOne_WhenConceptIsCreated() {
        long before = currentVersion(jdbcTemplate);

        creator.create(input("threading", "Threading", expression("Thread", "en", true)));

        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before + 1);
    }

    @Test
    void create_ShouldRaiseVersionByOne_WhenConceptHasNoExpression() {
        long before = currentVersion(jdbcTemplate);

        creator.create(input("threading", "Threading"));

        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before + 1);
    }

    @Test
    void create_ShouldRejectConcept_WhenKeyAlreadyExists() {
        creator.create(input("same-key", "First", expression("Thread", "en", true)));
        long before = currentVersion(jdbcTemplate);

        assertThatThrownBy(() -> creator.create(input("Same-Key", "Second")))
                .isInstanceOfSatisfying(ConceptOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ConceptFailureCode.CONCEPT_KEY_CONFLICT));

        assertThat(conceptCount()).isEqualTo(1);
        assertThat(expressionCount()).isEqualTo(1);
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void create_ShouldRejectConcept_WhenExpressionsRepeatAfterNormalization() {
        long before = currentVersion(jdbcTemplate);

        assertThatThrownBy(() -> creator.create(input("threading", "Threading",
                expression("Thread", "en", true), expression(" thread ", "vi", false))))
                .isInstanceOfSatisfying(ConceptOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ConceptFailureCode.DUPLICATE_CONCEPT_EXPRESSION));

        assertThat(conceptCount()).isZero();
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void create_ShouldRejectConcept_WhenValueIsInvalid() {
        long before = currentVersion(jdbcTemplate);

        assertThatThrownBy(() -> creator.create(input("Not a key", "Name")))
                .isInstanceOfSatisfying(ConceptOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ConceptFailureCode.VALIDATION_FAILED));

        assertThat(conceptCount()).isZero();
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void create_ShouldAcceptSameExpression_WhenItIsInTwoConcepts() {
        creator.create(input("threading", "Threading", expression("Thread", "en", true)));

        ConceptView second = creator.create(input("execution", "Execution", expression("thread", "en", true)));

        assertThat(second.expressions()).hasSize(1);
        assertThat(conceptCount()).isEqualTo(2);
        assertThat(expressionCount()).isEqualTo(2);
    }

    @Test
    void list_ShouldReturnEmptyList_WhenNoConceptExists() {
        assertThat(reader.list()).isEmpty();
    }

    @Test
    void list_ShouldReturnActiveAndInactiveConceptsOrderedByNameThenId_WhenConceptsExist() {
        insertConcept(jdbcTemplate, uuid(3), "key-c", "Beta", true);
        insertConcept(jdbcTemplate, uuid(2), "key-b", "Alpha", false);
        insertConcept(jdbcTemplate, uuid(1), "key-a", "Alpha", true);

        List<ConceptView> concepts = reader.list();

        assertThat(concepts).extracting(ConceptView::key)
                .containsExactly("key-a", "key-b", "key-c");
        assertThat(concepts).extracting(ConceptView::active)
                .containsExactly(true, false, true);
    }

    @Test
    void list_ShouldReturnExpressionsOrderedByNormalizedTextThenText_WhenConceptHasExpressions() {
        creator.create(input("letters", "Letters",
                expression("Gamma", "en", true), expression("alpha", "en", true), expression("Beta", "en", true)));

        List<ConceptView> concepts = reader.list();

        assertThat(concepts.get(0).expressions()).extracting(ConceptExpressionView::expression)
                .containsExactly("alpha", "Beta", "Gamma");
    }

    private static ConceptCreateInput input(
            String key, String displayName, ConceptExpressionInput... expressions) {
        return new ConceptCreateInput(key, displayName, "", true, List.of(expressions));
    }

    private static ConceptExpressionInput expression(String text, String languageHint, boolean autoExpand) {
        return new ConceptExpressionInput(text, languageHint, autoExpand);
    }

    private long conceptCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM retrieval_concept", Long.class);
    }

    private long expressionCount() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM retrieval_concept_expression", Long.class);
    }
}
