package vn.io.ajarline.topic.application;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.TopicTestData.insertContentItem;
import static vn.io.ajarline.support.TopicTestData.insertTopic;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.content.contract.TopicAssignmentPort;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;
import vn.io.ajarline.topic.contract.TopicFailureCode;
import vn.io.ajarline.topic.contract.TopicOperationException;
import vn.io.ajarline.topic.contract.TopicRemover;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class TopicAssignmentConcurrencyIT {

    private static final UUID TOPIC_ID = uuid(1);
    private static final UUID CONTENT_ID = uuid(10);

    @Autowired
    private TopicAssignmentPort assignments;

    @Autowired
    private TopicRemover remover;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("DELETE FROM content_item");
        jdbcTemplate.update("DELETE FROM topic");
        insertTopic(jdbcTemplate, TOPIC_ID, "java", "Java", true);
        insertContentItem(jdbcTemplate, CONTENT_ID, "concurrent-article");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void delete_ShouldReportTopicInUse_WhenAssignmentWasMadeFirst() throws Exception {
        List<Outcome> outcomes = raceOnTopicLock(
                () -> assignments.replaceAssignments(CONTENT_ID, List.of(TOPIC_ID)),
                () -> remover.delete(TOPIC_ID));

        assertThat(outcomes.get(0).failure()).isNull();
        assertThat(outcomes.get(1).failure())
                .isInstanceOfSatisfying(TopicOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(TopicFailureCode.TOPIC_IN_USE));
        assertThat(topicCount()).isEqualTo(1);
        assertThat(assignmentCount()).isEqualTo(1);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void assign_ShouldReportUnknownTopic_WhenDeletionWasMadeFirst() throws Exception {
        List<Outcome> outcomes = raceOnTopicLock(
                () -> remover.delete(TOPIC_ID),
                () -> assignments.replaceAssignments(CONTENT_ID, List.of(TOPIC_ID)));

        assertThat(outcomes.get(0).failure()).isNull();
        assertThat(outcomes.get(1).failure())
                .isInstanceOfSatisfying(ContentOperationException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ContentFailureCode.TOPIC_REFERENCE_NOT_FOUND));
        assertThat(topicCount()).isZero();
        assertThat(assignmentCount()).isZero();
    }

    /**
     * Runs two actions in two transactions. The first acts and then keeps its
     * transaction open. The second starts and must block on a database lock;
     * only after PostgreSQL reports that wait is the first one released.
     */
    private List<Outcome> raceOnTopicLock(Runnable first, Runnable second) throws Exception {
        CountDownLatch firstDone = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicInteger secondBackendPid = new AtomicInteger(-1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Outcome> firstFuture = null;
        Future<Outcome> secondFuture = null;
        try {
            firstFuture = executor.submit(() -> inTransaction(() -> {
                first.run();
                firstDone.countDown();
                awaitLatch(releaseFirst, 10, "The test did not release the first transaction.");
            }));
            awaitSignal(firstDone, firstFuture, "The first transaction did not finish its work.");

            secondFuture = executor.submit(() -> inTransaction(() -> {
                Integer pid = jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class);
                secondBackendPid.set(pid);
                secondStarted.countDown();
                second.run();
            }));
            awaitSignal(secondStarted, secondFuture, "The second transaction did not start.");
            awaitPostgreSqlLockWait(secondBackendPid.get());
            releaseFirst.countDown();

            return List.of(
                    firstFuture.get(15, TimeUnit.SECONDS),
                    secondFuture.get(15, TimeUnit.SECONDS));
        } finally {
            releaseFirst.countDown();
            stopExecutor(executor, firstFuture, secondFuture);
        }
    }

    private Outcome inTransaction(Runnable work) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setTimeout(10);
        try {
            transaction.executeWithoutResult(status -> work.run());
            return new Outcome(null);
        } catch (Throwable failure) {
            return new Outcome(failure);
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
            CountDownLatch latch, Future<Outcome> actor, String message) throws Exception {
        if (latch.await(5, TimeUnit.SECONDS)) {
            return;
        }
        if (actor.isDone()) {
            throw new IllegalStateException(message + " Actor outcome: " + actor.get());
        }
        throw new IllegalStateException(message);
    }

    /**
     * Waits until PostgreSQL reports that the given backend waits for a lock.
     * This replaces a sleep: the second transaction has reached the conflict
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

        throw new IllegalStateException("The second transaction did not wait for the database lock.");
    }

    private long topicCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM topic", Long.class);
    }

    private long assignmentCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM content_topic", Long.class);
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
                throw new IllegalStateException("Concurrent transactions did not stop before the timeout.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while stopping concurrent transactions.", exception);
        }
    }

    private record Outcome(Throwable failure) {
    }
}
