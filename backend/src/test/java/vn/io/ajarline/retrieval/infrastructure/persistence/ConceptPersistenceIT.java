package vn.io.ajarline.retrieval.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.io.ajarline.retrieval.application.EditableConcept;
import vn.io.ajarline.retrieval.application.ExpressionDraft;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.RetrievalTestData.deleteAllConcepts;
import static vn.io.ajarline.support.RetrievalTestData.draft;
import static vn.io.ajarline.support.RetrievalTestData.editable;
import static vn.io.ajarline.support.RetrievalTestData.newConcept;
import static vn.io.ajarline.support.RetrievalTestData.uuid;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
class ConceptPersistenceIT {

    private static final Instant CREATED_AT = Instant.parse("2026-11-21T00:00:00Z");
    private static final Instant CHANGED_AT = Instant.parse("2026-11-22T00:00:00Z");

    @Autowired
    private ConceptJpaRepository concepts;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transaction;
    private int nextExpressionId;

    @BeforeEach
    void resetRows() {
        deleteAllConcepts(jdbcTemplate);
        transaction = new TransactionTemplate(transactionManager);
        nextExpressionId = 1000;
    }

    @Test
    void saveAndFlush_ShouldStoreConceptAndExpressions_WhenConceptHasExpressions() {
        saveConcept(uuid(1), "threading", "Threading",
                draft("Thread", "en", true), draft("đa luồng", "vi", false));

        assertThat(count("retrieval_concept")).isEqualTo(1);
        assertThat(storedNormalizedTexts(uuid(1))).containsExactlyInAnyOrder("thread", "đa luồng");
    }

    @Test
    void findAllWithExpressions_ShouldReturnConceptsOrderedByNameThenId_WhenSeveralExist() {
        saveConcept(uuid(2), "key-b", "Alpha");
        saveConcept(uuid(1), "key-a", "Alpha");
        saveConcept(uuid(3), "key-c", "Beta");

        List<UUID> ids = concepts.findAllWithExpressions().stream()
                .map(ConceptEntity::getId)
                .toList();

        assertThat(ids).containsExactly(uuid(1), uuid(2), uuid(3));
    }

    @Test
    void findAllWithExpressions_ShouldLoadExpressions_WhenSessionIsClosed() {
        saveConcept(uuid(1), "threading", "Threading",
                draft("Thread", "en", true), draft("Multithreading", "en", true));

        List<ConceptEntity> loaded = concepts.findAllWithExpressions();

        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getExpressions()).hasSize(2);
    }

    @Test
    void findByIdForUpdate_ShouldReturnEmpty_WhenConceptDoesNotExist() {
        boolean found = transaction.execute(
                status -> concepts.findByIdForUpdate(uuid(99)).isPresent());

        assertThat(found).isFalse();
    }

    @Test
    void replaceEditableState_ShouldChangeRowInPlace_WhenOnlyLetterCaseChanges() {
        saveConcept(uuid(1), "threading", "Threading", draft("Thread", "en", true));

        replaceState(uuid(1), editable("Threading", true, draft("thread", "en", true)));

        assertThat(storedNormalizedTexts(uuid(1))).containsExactly("thread");
        assertThat(storedText(uuid(1), "thread")).isEqualTo("thread");
        assertThat(storedId(uuid(1), "thread")).isEqualTo(uuid(1000));
        assertThat(timestampOf("created_at", uuid(1), "thread")).isEqualTo(CREATED_AT);
        assertThat(timestampOf("updated_at", uuid(1), "thread")).isEqualTo(CHANGED_AT);
    }

    @Test
    void replaceEditableState_ShouldAddChangeAndRemoveRows_WhenTheSetChanges() {
        saveConcept(uuid(1), "letters", "Letters",
                draft("Alpha", "en", true), draft("Beta", "en", true), draft("Gamma", "en", true));

        replaceState(uuid(1), editable("Letters", true,
                draft("Beta", "en", true), draft("Gamma", "en", false), draft("Delta", "en", true)));

        assertThat(storedNormalizedTexts(uuid(1))).containsExactly("beta", "delta", "gamma");
        assertThat(timestampOf("updated_at", uuid(1), "beta")).isEqualTo(CREATED_AT);
        assertThat(timestampOf("updated_at", uuid(1), "gamma")).isEqualTo(CHANGED_AT);
        assertThat(timestampOf("created_at", uuid(1), "delta")).isEqualTo(CHANGED_AT);
    }

    @Test
    void replaceEditableState_ShouldRemoveEveryRow_WhenTheSetIsEmpty() {
        saveConcept(uuid(1), "letters", "Letters",
                draft("Alpha", "en", true), draft("Beta", "en", true));

        replaceState(uuid(1), editable("Letters", true));

        assertThat(storedNormalizedTexts(uuid(1))).isEmpty();
        assertThat(count("retrieval_concept")).isEqualTo(1);
    }

    @Test
    void hasSameEditableState_ShouldBeTrue_WhenExpressionsComeInAnotherOrder() {
        saveConcept(uuid(1), "threading", "Threading",
                draft("Thread", "en", true), draft("Multithreading", "en", true));

        boolean same = hasSameState(uuid(1), editable("Threading", true,
                draft("Multithreading", "en", true), draft("Thread", "en", true)));

        assertThat(same).isTrue();
    }

    @Test
    void hasSameEditableState_ShouldBeFalse_WhenOneAutoExpandFlagDiffers() {
        saveConcept(uuid(1), "threading", "Threading", draft("Thread", "en", true));

        boolean same = hasSameState(uuid(1), editable("Threading", true, draft("Thread", "en", false)));

        assertThat(same).isFalse();
    }

    @Test
    void hasSameEditableState_ShouldBeFalse_WhenAnExpressionIsMissingOrDisplayNameDiffers() {
        saveConcept(uuid(1), "threading", "Threading",
                draft("Thread", "en", true), draft("Multithreading", "en", true));

        assertThat(hasSameState(uuid(1), editable("Threading", true, draft("Thread", "en", true))))
                .isFalse();
        assertThat(hasSameState(uuid(1), editable("Other", true,
                draft("Thread", "en", true), draft("Multithreading", "en", true))))
                .isFalse();
    }

    @Test
    void delete_ShouldDeleteExpressions_WhenConceptIsDeleted() {
        saveConcept(uuid(1), "threading", "Threading", draft("Thread", "en", true));
        saveConcept(uuid(2), "execution", "Execution", draft("Task", "en", true));

        transaction.executeWithoutResult(
                status -> concepts.delete(concepts.findById(uuid(1)).orElseThrow()));

        assertThat(count("retrieval_concept")).isEqualTo(1);
        assertThat(count("retrieval_concept_expression")).isEqualTo(1);
        assertThat(storedNormalizedTexts(uuid(2))).containsExactly("task");
    }

    private void saveConcept(UUID id, String key, String displayName, ExpressionDraft... drafts) {
        transaction.executeWithoutResult(status -> concepts.saveAndFlush(ConceptEntity.create(
                id, newConcept(key, displayName, true, drafts), CREATED_AT, expressionIds())));
    }

    private void replaceState(UUID id, EditableConcept editable) {
        transaction.executeWithoutResult(status -> {
            ConceptEntity concept = concepts.findByIdForUpdate(id).orElseThrow();
            concept.replaceEditableState(editable, CHANGED_AT, expressionIds());
            concepts.flush();
        });
    }

    private boolean hasSameState(UUID id, EditableConcept editable) {
        return transaction.execute(
                status -> concepts.findById(id).orElseThrow().hasSameEditableState(editable));
    }

    private Supplier<UUID> expressionIds() {
        return () -> uuid(nextExpressionId++);
    }

    private List<String> storedNormalizedTexts(UUID conceptId) {
        return jdbcTemplate.queryForList("""
                SELECT normalized_expression FROM retrieval_concept_expression
                WHERE concept_id = ? ORDER BY normalized_expression
                """, String.class, conceptId);
    }

    private String storedText(UUID conceptId, String normalized) {
        return jdbcTemplate.queryForObject("""
                SELECT expression FROM retrieval_concept_expression
                WHERE concept_id = ? AND normalized_expression = ?
                """, String.class, conceptId, normalized);
    }

    private UUID storedId(UUID conceptId, String normalized) {
        return jdbcTemplate.queryForObject("""
                SELECT id FROM retrieval_concept_expression
                WHERE concept_id = ? AND normalized_expression = ?
                """, UUID.class, conceptId, normalized);
    }

    private Instant timestampOf(String column, UUID conceptId, String normalized) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM retrieval_concept_expression"
                        + " WHERE concept_id = ? AND normalized_expression = ?",
                OffsetDateTime.class, conceptId, normalized).toInstant();
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
