package vn.io.ajarline.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.io.ajarline.content.contract.ArticleRemover;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.content.contract.ContentPublicationAccess;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.ContentTestData;
import vn.io.ajarline.support.MutableClock;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class ArticleDeletionConcurrencyIT {

    private static final UUID ARTICLE = uuid(1);

    @Autowired
    private ArticleRemover remover;

    @Autowired
    private ContentPublicationAccess publication;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void insertDraft() {
        jdbcTemplate.update("DELETE FROM content_item");
        ContentTestData.insertArticle(
                jdbcTemplate, ARTICLE, "racing-article", "Racing article", PublicationStatus.DRAFT);
    }

    @AfterEach
    void clear() {
        jdbcTemplate.update("DELETE FROM content_item");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void delete_ShouldFailAndKeepTheArticle_WhenPublishCommitsFirst() throws Exception {
        CountDownLatch publisherHoldsLock = new CountDownLatch(1);
        CountDownLatch releasePublisher = new CountDownLatch(1);
        CountDownLatch deleterStarted = new CountDownLatch(1);
        AtomicInteger deleterPid = new AtomicInteger(-1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Throwable> publisher = inTransaction(executor, () -> {
                publication.markPublished(ARTICLE, 1, clock.instant());
                publisherHoldsLock.countDown();
                await(releasePublisher);
            });
            await(publisherHoldsLock);
            Future<Throwable> deleter = inTransaction(executor, () -> {
                deleterPid.set(jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class));
                deleterStarted.countDown();
                remover.delete(ARTICLE, 1);
            });
            await(deleterStarted);
            awaitLockWait(deleterPid.get());
            releasePublisher.countDown();

            assertThat(publisher.get(15, TimeUnit.SECONDS)).isNull();
            assertThat(deleter.get(15, TimeUnit.SECONDS))
                    .isInstanceOfSatisfying(ContentOperationException.class, failure ->
                            assertThat(failure.code()).isEqualTo(ContentFailureCode.REVISION_CONFLICT));
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM content_item WHERE id = ?", String.class, ARTICLE))
                    .isEqualTo("PUBLISHED");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM article WHERE content_id = ?", Long.class, ARTICLE))
                    .isEqualTo(1);
        } finally {
            releasePublisher.countDown();
            executor.shutdownNow();
        }
    }

    /**
     * Runs the work in one transaction. Returns the failure, or null when the work succeeded.
     */
    private Future<Throwable> inTransaction(ExecutorService executor, Runnable work) {
        return executor.submit(() -> {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.setTimeout(10);
            try {
                transaction.executeWithoutResult(status -> work.run());
                return null;
            } catch (Throwable failure) {
                return failure;
            }
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("A latch was not released in time.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The test was interrupted.", exception);
        }
    }

    private void awaitLockWait(int backendPid) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            Boolean waiting = jdbcTemplate.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1 FROM pg_stat_activity
                         WHERE pid = ? AND wait_event_type = 'Lock')
                    """, Boolean.class, backendPid);
            if (Boolean.TRUE.equals(waiting)) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
        throw new IllegalStateException("The deleter did not wait for the row lock.");
    }
}
