package vn.io.ajarline.content.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
import vn.io.ajarline.content.contract.ArticleContentInput;
import vn.io.ajarline.content.contract.ArticleCreator;
import vn.io.ajarline.content.contract.ArticleEditor;
import vn.io.ajarline.content.contract.ArticleReader;
import vn.io.ajarline.content.contract.ArticleView;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemJpaRepository;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.MutableClock;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class ArticleReplacementConcurrencyIT {

    private static final Instant CREATED_AT =
            Instant.parse("2026-10-26T00:00:00Z");

    @Autowired
    private ArticleCreator creator;

    @Autowired
    private ArticleReader reader;

    @Autowired
    private ArticleEditor editor;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ContentItemJpaRepository contentItems;

    @BeforeEach
    void resetDatabaseAndClock() {
        jdbcTemplate.update("DELETE FROM content_item");
        clock.set(CREATED_AT);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void replace_ShouldAllowOneWriter_WhenTwoWritersUseSameRevision()
            throws Exception {
        // Given
        ArticleView created = creator.create(input("concurrent-article", "Initial"));
        AtomicInteger secondWriterBackendPid = new AtomicInteger(-1);
        CountDownLatch firstLockAcquired = new CountDownLatch(1);
        CountDownLatch secondWriterReachedLockBoundary = new CountDownLatch(1);
        CountDownLatch releaseFirstWriter = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<UpdateAttempt> first = null;
        Future<UpdateAttempt> second = null;

        try {
            first = executor.submit(firstWriterTask(
                    "writer-a",
                    created.id(),
                    input("concurrent-article", "Writer A"),
                    firstLockAcquired,
                    releaseFirstWriter
            ));

            awaitActorSignal(
                    firstLockAcquired,
                    first,
                    5,
                    "The first writer did not acquire the row lock."
            );

            second = executor.submit(secondWriterTask(
                    "writer-b",
                    created.id(),
                    input("concurrent-article", "Writer B"),
                    secondWriterBackendPid,
                    secondWriterReachedLockBoundary
            ));

            // When
            awaitActorSignal(
                    secondWriterReachedLockBoundary,
                    second,
                    5,
                    "The second writer did not reach the row-lock boundary."
            );
            awaitPostgreSqlLockWait(secondWriterBackendPid.get());
            releaseFirstWriter.countDown();

            UpdateAttempt firstAttempt = first.get(15, TimeUnit.SECONDS);
            UpdateAttempt secondAttempt = second.get(15, TimeUnit.SECONDS);
            List<UpdateAttempt> attempts = List.of(
                    firstAttempt,
                    secondAttempt
            );

            // Then
            assertThat(attempts)
                    .filteredOn(attempt -> attempt.unexpectedFailure() != null)
                    .as("unexpected actor failures")
                    .isEmpty();
            assertThat(attempts)
                    .filteredOn(UpdateAttempt::succeeded)
                    .hasSize(1);
            assertThat(attempts)
                    .extracting(UpdateAttempt::failure)
                    .filteredOn(Objects::nonNull)
                    .containsExactly(ContentFailureCode.REVISION_CONFLICT);

            ArticleView winner = attempts.stream()
                    .filter(UpdateAttempt::succeeded)
                    .map(UpdateAttempt::view)
                    .findFirst()
                    .orElseThrow();
            ArticleView persisted = reader.get(created.id());
            assertThat(persisted.revision()).isEqualTo(2);
            assertThat(persisted.bodyMarkdown())
                    .isEqualTo(winner.bodyMarkdown());
            assertThat(rowCount("content_item")).isEqualTo(1);
            assertThat(rowCount("article")).isEqualTo(1);
        } finally {
            releaseFirstWriter.countDown();
            stopExecutor(executor, first, second);
        }
    }


    private static void awaitLatch(
            CountDownLatch latch,
            long timeoutSeconds,
            String timeoutMessage
    ) {
        try {
            if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new IllegalStateException(timeoutMessage);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Concurrency test was interrupted.",
                    exception
            );
        }
    }


    private static void awaitActorSignal(
            CountDownLatch latch,
            Future<UpdateAttempt> actor,
            long timeoutSeconds,
            String timeoutMessage
    ) {
        try {
            if (latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                return;
            }

            if (actor.isDone()) {
                UpdateAttempt attempt = actor.get();
                if (attempt.unexpectedFailure() != null) {
                    throw new IllegalStateException(
                            timeoutMessage,
                            attempt.unexpectedFailure()
                    );
                }
                throw new IllegalStateException(
                        timeoutMessage + " Actor outcome: " + attempt
                );
            }

            throw new IllegalStateException(timeoutMessage);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Concurrency test was interrupted.",
                    exception
            );
        } catch (ExecutionException exception) {
            throw new IllegalStateException(
                    timeoutMessage,
                    exception.getCause()
            );
        }
    }


    private void awaitPostgreSqlLockWait(int backendPid) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

        while (System.nanoTime() < deadline) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException(
                        "Concurrency test was interrupted while observing the lock."
                );
            }

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

        throw new IllegalStateException(
                "The second writer did not wait for the PostgreSQL row lock."
        );
    }


    private Callable<UpdateAttempt> firstWriterTask(
            String actorId,
            UUID id,
            ArticleContentInput input,
            CountDownLatch lockAcquired,
            CountDownLatch releaseWriter
    ) {
        return () -> executeUpdate(actorId, () -> {
            contentItems.findByIdForUpdate(id)
                    .orElseThrow(() -> new IllegalStateException(
                            "The Article disappeared before the lock was acquired."
                    ));
            lockAcquired.countDown();
            awaitLatch(
                    releaseWriter,
                    10,
                    "The test did not release the first writer."
            );
            return editor.replace(id, 1, input);
        });
    }

    private Callable<UpdateAttempt> secondWriterTask(
            String actorId,
            UUID id,
            ArticleContentInput input,
            AtomicInteger backendPid,
            CountDownLatch reachedLockBoundary
    ) {
        return () -> executeUpdate(actorId, () -> {
            Integer currentBackendPid = jdbcTemplate.queryForObject(
                    "SELECT pg_backend_pid()",
                    Integer.class
            );
            backendPid.set(currentBackendPid);
            reachedLockBoundary.countDown();
            return editor.replace(id, 1, input);
        });
    }

    private UpdateAttempt executeUpdate(
            String actorId,
            Supplier<ArticleView> update
    ) {
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);
        transaction.setTimeout(10);

        try {
            return transaction.execute(status -> UpdateAttempt.success(
                    actorId,
                    update.get()
            ));
        } catch (ContentOperationException exception) {
            return UpdateAttempt.failure(actorId, exception.code());
        } catch (Throwable failure) {
            return UpdateAttempt.unexpected(actorId, failure);
        }
    }


    private static void stopExecutor(
            ExecutorService executor,
            Future<?>... futures
    ) {
        for (Future<?> future : futures) {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }

        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Concurrent writers did not stop before the timeout."
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while stopping concurrent writers.",
                    exception
            );
        }
    }

    private record UpdateAttempt(
            String actorId,
            String threadName,
            ArticleView view,
            ContentFailureCode failure,
            Throwable unexpectedFailure
    ) {
        private static UpdateAttempt success(
                String actorId,
                ArticleView view
        ) {
            return new UpdateAttempt(
                    actorId,
                    Thread.currentThread().getName(),
                    view,
                    null,
                    null
            );
        }

        private static UpdateAttempt failure(
                String actorId,
                ContentFailureCode failure
        ) {
            return new UpdateAttempt(
                    actorId,
                    Thread.currentThread().getName(),
                    null,
                    failure,
                    null
            );
        }

        private static UpdateAttempt unexpected(
                String actorId,
                Throwable failure
        ) {
            return new UpdateAttempt(
                    actorId,
                    Thread.currentThread().getName(),
                    null,
                    null,
                    failure
            );
        }

        private boolean succeeded() {
            return view != null
                    && failure == null
                    && unexpectedFailure == null;
        }
    }

    private ArticleContentInput input(String slug, String bodyMarkdown) {
        return new ArticleContentInput(
                "Java threads",
                slug,
                null,
                bodyMarkdown
        );
    }

    private long rowCount(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table,
                Long.class
        );
    }
}
