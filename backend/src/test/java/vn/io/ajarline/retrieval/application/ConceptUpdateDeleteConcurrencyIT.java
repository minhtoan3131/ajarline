package vn.io.ajarline.retrieval.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import vn.io.ajarline.retrieval.contract.ConceptCreateInput;
import vn.io.ajarline.retrieval.contract.ConceptCreator;
import vn.io.ajarline.retrieval.contract.ConceptEditor;
import vn.io.ajarline.retrieval.contract.ConceptExpressionInput;
import vn.io.ajarline.retrieval.contract.ConceptFailureCode;
import vn.io.ajarline.retrieval.contract.ConceptOperationException;
import vn.io.ajarline.retrieval.contract.ConceptRemover;
import vn.io.ajarline.retrieval.contract.ConceptUpdateInput;
import vn.io.ajarline.support.ConcurrentActors.Outcome;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.PostgreSqlLockWait;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.ConcurrentActors.awaitLatch;
import static vn.io.ajarline.support.ConcurrentActors.awaitSignal;
import static vn.io.ajarline.support.ConcurrentActors.inTransaction;
import static vn.io.ajarline.support.ConcurrentActors.stop;
import static vn.io.ajarline.support.RetrievalTestData.currentVersion;
import static vn.io.ajarline.support.RetrievalTestData.deleteAllConcepts;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class ConceptUpdateDeleteConcurrencyIT {

    @Autowired
    private ConceptCreator creator;

    @Autowired
    private ConceptEditor editor;

    @Autowired
    private ConceptRemover remover;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void resetDatabase() {
        deleteAllConcepts(jdbcTemplate);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void delete_ShouldWaitAndSucceed_WhenAnUpdateHoldsTheConceptFirst() throws Exception {
        UUID id = createConcept();
        long before = currentVersion(jdbcTemplate);

        List<Outcome> outcomes = race(
                () -> editor.replace(id, rename()),
                () -> {
                    remover.delete(id);
                    return null;
                });

        assertThat(outcomes).allMatch(Outcome::succeeded);
        assertThat(conceptCount()).isZero();
        assertThat(orphanExpressionCount()).isZero();
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before + 2);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void update_ShouldReportNotFound_WhenADeleteHoldsTheConceptFirst() throws Exception {
        UUID id = createConcept();
        long before = currentVersion(jdbcTemplate);

        List<Outcome> outcomes = race(
                () -> {
                    remover.delete(id);
                    return null;
                },
                () -> editor.replace(id, rename()));

        assertThat(outcomes.get(0).succeeded()).isTrue();
        assertThat(outcomes.get(1).failure())
                .isInstanceOfSatisfying(ConceptOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ConceptFailureCode.CONCEPT_NOT_FOUND));
        assertThat(conceptCount()).isZero();
        assertThat(orphanExpressionCount()).isZero();
        assertThat(currentVersion(jdbcTemplate)).isEqualTo(before + 1);
    }

    /**
     * Runs two actors. The holder finishes its work but keeps its transaction open. The waiter
     * starts then and must wait on the database. The test releases the holder only after
     * PostgreSQL reports that the waiter is blocked on a lock.
     */
    private List<Outcome> race(Supplier<Object> holder, Supplier<Object> waiter) throws Exception {
        CountDownLatch holderDone = new CountDownLatch(1);
        CountDownLatch waiterStarted = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);
        AtomicInteger waiterBackendPid = new AtomicInteger(-1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Outcome> first = null;
        Future<Outcome> second = null;

        try {
            first = executor.submit(() -> inTransaction(transactionManager, () -> {
                Object result = holder.get();
                holderDone.countDown();
                awaitLatch(releaseHolder, 10, "The test did not release the first actor.");
                return result;
            }));
            awaitSignal(holderDone, first, "The first actor did not finish its operation.");

            second = executor.submit(() -> inTransaction(transactionManager, () -> {
                waiterBackendPid.set(PostgreSqlLockWait.backendPid(jdbcTemplate));
                waiterStarted.countDown();
                return waiter.get();
            }));
            awaitSignal(waiterStarted, second, "The second actor did not start.");
            PostgreSqlLockWait.await(jdbcTemplate, waiterBackendPid.get());
            releaseHolder.countDown();

            return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        } finally {
            releaseHolder.countDown();
            stop(executor, first, second);
        }
    }

    private UUID createConcept() {
        UUID id = creator.create(new ConceptCreateInput(
                "threading", "Threading", null, true,
                List.of(new ConceptExpressionInput("Thread", "en", true),
                        new ConceptExpressionInput("Task", "en", true)))).id();
        assertThat(orphanExpressionCount()).isZero();
        assertThat(expressionCount()).isEqualTo(2);
        return id;
    }

    private static ConceptUpdateInput rename() {
        return new ConceptUpdateInput(
                "Renamed", null, true, List.of(new ConceptExpressionInput("Thread", "en", true)));
    }

    private long conceptCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM retrieval_concept", Long.class);
    }

    private long expressionCount() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM retrieval_concept_expression", Long.class);
    }

    private long orphanExpressionCount() {
        return jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM retrieval_concept_expression expression
                WHERE NOT EXISTS (
                    SELECT 1 FROM retrieval_concept concept WHERE concept.id = expression.concept_id
                )
                """, Long.class);
    }
}
