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
import vn.io.ajarline.retrieval.contract.ConceptFailureCode;
import vn.io.ajarline.retrieval.contract.ConceptOperationException;
import vn.io.ajarline.support.ConcurrentActors.Outcome;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.PostgreSqlLockWait;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
class ConceptKeyConcurrencyIT {

    private static final String KEY = "concurrent-concept";

    @Autowired
    private ConceptCreator creator;

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
    void create_ShouldAllowOneCreator_WhenTwoCreatorsUseSameKey() throws Exception {
        long before = currentVersion(jdbcTemplate);
        CountDownLatch firstCreated = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger secondBackendPid = new AtomicInteger(-1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Outcome> first = null;
        Future<Outcome> second = null;

        try {
            first = executor.submit(() -> inTransaction(transactionManager, () -> {
                Object created = creator.create(input("Creator A"));
                firstCreated.countDown();
                awaitLatch(releaseFirst, 10, "The test did not release the first creator.");
                return created;
            }));
            awaitSignal(firstCreated, first, "The first creator did not insert the Concept.");

            second = executor.submit(() -> inTransaction(transactionManager, () -> {
                secondBackendPid.set(PostgreSqlLockWait.backendPid(jdbcTemplate));
                secondStarted.countDown();
                return creator.create(input("Creator B"));
            }));
            awaitSignal(secondStarted, second, "The second creator did not start.");
            PostgreSqlLockWait.await(jdbcTemplate, secondBackendPid.get());
            releaseFirst.countDown();

            Outcome firstOutcome = first.get(15, TimeUnit.SECONDS);
            Outcome secondOutcome = second.get(15, TimeUnit.SECONDS);
            List<Outcome> outcomes = List.of(firstOutcome, secondOutcome);

            assertThat(outcomes).filteredOn(Outcome::succeeded).hasSize(1);
            assertThat(outcomes).filteredOn(outcome -> !outcome.succeeded())
                    .extracting(Outcome::failure)
                    .singleElement()
                    .isInstanceOfSatisfying(ConceptOperationException.class,
                            exception -> assertThat(exception.code())
                                    .isEqualTo(ConceptFailureCode.CONCEPT_KEY_CONFLICT));
            assertThat(conceptCount()).isEqualTo(1);
            assertThat(currentVersion(jdbcTemplate)).isEqualTo(before + 1);
        } finally {
            releaseFirst.countDown();
            stop(executor, first, second);
        }
    }

    private static ConceptCreateInput input(String displayName) {
        return new ConceptCreateInput(KEY, displayName, null, true, List.of());
    }

    private long conceptCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM retrieval_concept", Long.class);
    }
}
