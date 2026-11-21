package vn.io.ajarline.retrieval.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.io.ajarline.support.RetrievalTestData.deleteAllConcepts;
import static vn.io.ajarline.support.RetrievalTestData.insertConcept;
import static vn.io.ajarline.support.RetrievalTestData.insertExpression;
import static vn.io.ajarline.support.RetrievalTestData.uuid;
import static vn.io.ajarline.support.RetrievalTestData.currentVersion;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
class ConceptDatabaseConstraintIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void removeRows() {
        deleteAllConcepts(jdbcTemplate);
    }

    @Test
    void insert_ShouldRejectDuplicateKey_WhenKeyAlreadyExists() {
        insertConcept(jdbcTemplate, uuid(1), "same-key", "First", true);

        assertThatThrownBy(() -> insertConcept(jdbcTemplate, uuid(2), "same-key", "Second", true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_retrieval_concept_key");
    }

    @Test
    void insert_ShouldAcceptDuplicateDisplayName_WhenKeysDiffer() {
        insertConcept(jdbcTemplate, uuid(1), "first-key", "Same name", true);

        insertConcept(jdbcTemplate, uuid(2), "second-key", "Same name", true);

        assertThat(count("retrieval_concept")).isEqualTo(2);
    }

    @Test
    void insert_ShouldRejectKey_WhenKeyIsNotLowercase() {
        assertThatThrownBy(() -> insertConcept(jdbcTemplate, uuid(3), "Not-Lowercase", "Name", true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_retrieval_concept_key_lowercase");
    }

    @Test
    void insert_ShouldRejectKey_WhenKeyIsBlank() {
        assertThatThrownBy(() -> insertConcept(jdbcTemplate, uuid(4), "   ", "Name", true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_retrieval_concept_key_not_blank");
    }

    @Test
    void insert_ShouldRejectKey_WhenKeyExceedsDatabaseLimit() {
        String key = "a".repeat(161);

        assertThatThrownBy(() -> insertConcept(jdbcTemplate, uuid(5), key, "Name", true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("character varying(160)");
    }

    @Test
    void insert_ShouldRejectDisplayName_WhenDisplayNameIsBlank() {
        assertThatThrownBy(() -> insertConcept(jdbcTemplate, uuid(6), "blank-name", "  ", true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_retrieval_concept_display_name_not_blank");
    }

    @Test
    void insert_ShouldRejectRow_WhenActiveIsNull() {
        assertThatThrownBy(() -> insertConcept(jdbcTemplate, uuid(7), "null-active", "Name", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("active");
    }

    @Test
    void insertExpression_ShouldRejectRow_WhenConceptDoesNotExist() {
        assertThatThrownBy(() -> insertExpression(
                jdbcTemplate, uuid(10), uuid(99), "Thread", "thread", "en", true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_retrieval_concept_expression_concept_id");
    }

    @Test
    void insertExpression_ShouldRejectRow_WhenNormalizedTextRepeatsInOneConcept() {
        insertConcept(jdbcTemplate, uuid(1), "threading", "Threading", true);
        insertExpression(jdbcTemplate, uuid(10), uuid(1), "Thread", "thread", "en", true);

        assertThatThrownBy(() -> insertExpression(
                jdbcTemplate, uuid(11), uuid(1), " thread ", "thread", "en", true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_retrieval_concept_expression_normalized");
    }

    @Test
    void insertExpression_ShouldAcceptRow_WhenNormalizedTextIsInTwoConcepts() {
        insertConcept(jdbcTemplate, uuid(1), "threading", "Threading", true);
        insertConcept(jdbcTemplate, uuid(2), "execution", "Execution", true);
        insertExpression(jdbcTemplate, uuid(10), uuid(1), "Thread", "thread", "en", true);

        insertExpression(jdbcTemplate, uuid(11), uuid(2), "Thread", "thread", "en", true);

        assertThat(count("retrieval_concept_expression")).isEqualTo(2);
    }

    @Test
    void insertExpression_ShouldRejectRow_WhenLanguageHintIsNotAllowed() {
        insertConcept(jdbcTemplate, uuid(1), "threading", "Threading", true);

        assertThatThrownBy(() -> insertExpression(
                jdbcTemplate, uuid(10), uuid(1), "Thread", "thread", "fr", true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_retrieval_concept_expression_language_hint");
    }

    @Test
    void insertExpression_ShouldAcceptRow_WhenLanguageHintIsNull() {
        insertConcept(jdbcTemplate, uuid(1), "threading", "Threading", true);

        insertExpression(jdbcTemplate, uuid(10), uuid(1), "Thread", "thread", null, true);

        assertThat(count("retrieval_concept_expression")).isEqualTo(1);
    }

    @Test
    void insertExpression_ShouldRejectRow_WhenTextIsBlank() {
        insertConcept(jdbcTemplate, uuid(1), "threading", "Threading", true);

        assertThatThrownBy(() -> insertExpression(
                jdbcTemplate, uuid(10), uuid(1), "   ", "thread", "en", true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_retrieval_concept_expression_expression_not_blank");
        assertThatThrownBy(() -> insertExpression(
                jdbcTemplate, uuid(11), uuid(1), "Thread", "   ", "en", true))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_retrieval_concept_expression_normalized_not_blank");
    }

    @Test
    void deleteConcept_ShouldDeleteItsExpressions_WhenConceptIsDeleted() {
        insertConcept(jdbcTemplate, uuid(1), "threading", "Threading", true);
        insertConcept(jdbcTemplate, uuid(2), "execution", "Execution", true);
        insertExpression(jdbcTemplate, uuid(10), uuid(1), "Thread", "thread", "en", true);
        insertExpression(jdbcTemplate, uuid(11), uuid(2), "Task", "task", "en", true);

        jdbcTemplate.update("DELETE FROM retrieval_concept WHERE id = ?", uuid(1));

        assertThat(count("retrieval_concept")).isEqualTo(1);
        assertThat(count("retrieval_concept_expression")).isEqualTo(1);
    }

    @Test
    void deleteConcept_ShouldNotChangeVersion_WhenOnlyTheDatabaseRunsTheDelete() {
        insertConcept(jdbcTemplate, uuid(1), "threading", "Threading", true);
        long before = currentVersion(jdbcTemplate);

        jdbcTemplate.update("DELETE FROM retrieval_concept WHERE id = ?", uuid(1));

        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void versionRow_ShouldBeTheOnlyRow_WhenMigrationCompletes() {
        assertThat(count("retrieval_vocabulary_state")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT id FROM retrieval_vocabulary_state", Integer.class)).isEqualTo(1);
        assertThat(currentVersion(jdbcTemplate)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void insertVersionRow_ShouldBeRejected_WhenIdIsNotOne() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO retrieval_vocabulary_state(id, version, updated_at)
                VALUES (2, 1, now())
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_retrieval_vocabulary_state_singleton");
    }

    @Test
    void updateVersion_ShouldBeRejected_WhenVersionIsNotPositive() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE retrieval_vocabulary_state SET version = 0"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_retrieval_vocabulary_state_version_positive");
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
