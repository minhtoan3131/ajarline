package vn.io.ajarline.retrieval.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.io.ajarline.retrieval.infrastructure.persistence.ConceptEntity;
import vn.io.ajarline.retrieval.infrastructure.persistence.ConceptJpaRepository;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.io.ajarline.support.RetrievalTestData.deleteAllConcepts;
import static vn.io.ajarline.support.RetrievalTestData.draft;
import static vn.io.ajarline.support.RetrievalTestData.newConcept;
import static vn.io.ajarline.support.RetrievalTestData.uuid;
import static vn.io.ajarline.support.RetrievalTestData.currentVersion;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
class VocabularyVersionIT {

    private static final Instant NOW = Instant.parse("2026-11-21T00:00:00Z");

    @Autowired
    private VocabularyVersion vocabularyVersion;

    @Autowired
    private ConceptJpaRepository concepts;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transaction;

    @BeforeEach
    void resetRows() {
        deleteAllConcepts(jdbcTemplate);
        transaction = new TransactionTemplate(transactionManager);
    }

    @Test
    void advance_ShouldRaiseVersionByOne_WhenCalledInTransaction() {
        long before = currentVersion(jdbcTemplate);

        transaction.executeWithoutResult(status -> vocabularyVersion.advance(NOW));

        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before + 1);
    }

    @Test
    void advance_ShouldRaiseVersionTwice_WhenCalledTwiceInTransaction() {
        long before = currentVersion(jdbcTemplate);

        transaction.executeWithoutResult(status -> {
            vocabularyVersion.advance(NOW);
            vocabularyVersion.advance(NOW);
        });

        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before + 2);
    }

    @Test
    void advance_ShouldFail_WhenNoTransactionIsActive() {
        long before = currentVersion(jdbcTemplate);

        assertThatThrownBy(() -> vocabularyVersion.advance(NOW))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void advance_ShouldFail_WhenVersionRowIsMissing() {
        long before = currentVersion(jdbcTemplate);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            jdbcTemplate.update("DELETE FROM retrieval_vocabulary_state");
            vocabularyVersion.advance(NOW);
        })).isInstanceOf(IllegalStateException.class);

        // The failed transaction rolled back, so the row is back.
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
    }

    @Test
    void advance_ShouldRollBackWithConcept_WhenALaterStepFails() {
        long before = currentVersion(jdbcTemplate);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            concepts.saveAndFlush(ConceptEntity.create(
                    uuid(1), newConcept("threading", "Threading", true, draft("Thread", "en", true)),
                    NOW, () -> uuid(1000)));
            vocabularyVersion.advance(NOW);
            throw new IllegalStateException("A later step failed.");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM retrieval_concept", Long.class)).isZero();
    }
}
