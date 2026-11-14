package vn.io.ajarline.topic.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;
import vn.io.ajarline.topic.contract.TopicCreateInput;
import vn.io.ajarline.topic.contract.TopicCreator;
import vn.io.ajarline.topic.contract.TopicFailureCode;
import vn.io.ajarline.topic.contract.TopicOperationException;
import vn.io.ajarline.topic.contract.TopicView;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class TopicSlugConcurrencyIT {

    private static final String SLUG = "concurrent-topic";

    @Autowired
    private TopicCreator creator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("DELETE FROM content_item");
        jdbcTemplate.update("DELETE FROM topic");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void create_ShouldAllowOneCreator_WhenTwoCreatorsUseSameSlug() throws Exception {
        CountDownLatch firstInserted = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger secondBackendPid = new AtomicInteger(-1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<CreateAttempt> first = null;
        Future<CreateAttempt> second = null;

        try {
            first = executor.submit(() -> inTransaction("creator-a", () -> {
                TopicView created = creator.create(input("Creator A"));
                firstInserted.countDown();
                awaitLatch(releaseFirst, 10, "The test did not release the first creator.");
                return created;
            }));
            awaitSignal(firstInserted, first, "The first creator did not insert the Topic.");

            second = executor.submit(() -> inTransaction("creator-b", () -> {
                Integer pid = jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class);
                secondBackendPid.set(pid);
                secondStarted.countDown();
                return creator.create(input("Creator B"));
            }));
            awaitSignal(secondStarted, second, "The second creator did not start.");
            awaitPostgreSqlLockWait(secondBackendPid.get());
            releaseFirst.countDown();

            List<CreateAttempt> attempts = List.of(
                    first.get(15, TimeUnit.SECONDS),
                    second.get(15, TimeUnit.SECONDS));

            assertThat(attempts)
                    .filteredOn(attempt -> attempt.unexpectedFailure() != null)
                    .as("unexpected creator failures")
                    .isEmpty();
            assertThat(attempts)
                    .filteredOn(CreateAttempt::succeeded)
                    .hasSize(1);
            assertThat(attempts)
                    .extracting(CreateAttempt::failure)
                    .filteredOn(Objects::nonNull)
                    .containsExactly(TopicFailureCode.SLUG_CONFLICT);

            TopicView winner = attempts.stream()
                    .filter(CreateAttempt::succeeded)
                    .map(CreateAttempt::view)
                    .findFirst()
                    .orElseThrow();
            assertThat(topicCount()).isEqualTo(1);
            assertThat(storedName()).isEqualTo(winner.name());
        } finally {
            releaseFirst.countDown();
            stopExecutor(executor, first, second);
        }
    }

    private static TopicCreateInput input(String name) {
        return new TopicCreateInput(SLUG, name, null, true);
    }

    private CreateAttempt inTransaction(String actorId, Supplier<TopicView> work) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setTimeout(10);

        try {
            return transaction.execute(status -> CreateAttempt.success(actorId, work.get()));
        } catch (TopicOperationException exception) {
            return CreateAttempt.failure(actorId, exception.code());
        } catch (Throwable failure) {
            return CreateAttempt.unexpected(actorId, failure);
        }
    }

    private static void awaitLatch(CountDownLatch latch, long timeoutSeconds, String message) {
        try {
            if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new IllegalStateException(message);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrency test was interrupted.", exception);
        }
    }

    private static void awaitSignal(
            CountDownLatch latch, Future<CreateAttempt> actor, String message) throws Exception {
        if (latch.await(5, TimeUnit.SECONDS)) {
            return;
        }
        if (actor.isDone()) {
            throw new IllegalStateException(message + " Actor outcome: " + actor.get());
        }
        throw new IllegalStateException(message);
    }

    /**
     * Waits until PostgreSQL reports that the given backend is waiting for a lock.
     * This replaces a sleep: the second creator has reached the conflict point
     * only when the database says it is blocked.
     */
    private void awaitPostgreSqlLockWait(int backendPid) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

        while (System.nanoTime() < deadline) {
            Boolean waitingForLock = jdbcTemplate.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1
                        FROM pg_stat_activity
                        WHERE pid = ?
                          AND wait_event_type = 'Lock'
                    )
                    """, Boolean.class, backendPid);

            if (Boolean.TRUE.equals(waitingForLock)) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }

        throw new IllegalStateException("The second creator did not wait for the database lock.");
    }

    private long topicCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM topic", Long.class);
    }

    private String storedName() {
        return jdbcTemplate.queryForObject(
                "SELECT name FROM topic WHERE slug = ?", String.class, SLUG);
    }

    private static void stopExecutor(ExecutorService executor, Future<?>... futures) {
        for (Future<?> future : futures) {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }

        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent creators did not stop before the timeout.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while stopping concurrent creators.", exception);
        }
    }

    private record CreateAttempt(
            String actorId,
            TopicView view,
            TopicFailureCode failure,
            Throwable unexpectedFailure
    ) {
        private static CreateAttempt success(String actorId, TopicView view) {
            return new CreateAttempt(actorId, view, null, null);
        }

        private static CreateAttempt failure(String actorId, TopicFailureCode failure) {
            return new CreateAttempt(actorId, null, failure, null);
        }

        private static CreateAttempt unexpected(String actorId, Throwable failure) {
            return new CreateAttempt(actorId, null, null, failure);
        }

        private boolean succeeded() {
            return view != null && failure == null && unexpectedFailure == null;
        }
    }
}
