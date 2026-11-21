package vn.io.ajarline.learning.application;

import static org.assertj.core.api.Assertions.assertThat;
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
import vn.io.ajarline.content.contract.ArticleContentInput;
import vn.io.ajarline.content.contract.ArticleEditor;
import vn.io.ajarline.content.contract.ArticlePlacement;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.content.contract.ContentRecordWriter;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.learning.contract.ModuleRemover;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.ContentTestData;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class ModuleDeletionConcurrencyIT {

    private static final UUID SERIES = uuid(1);
    private static final UUID MODULE = uuid(2);
    private static final UUID ARTICLE = uuid(3);

    @Autowired
    private ModuleRemover moduleRemover;

    @Autowired
    private ArticleEditor articleEditor;

    @Autowired
    private ContentRecordWriter recordWriter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void insertRows() {
        LearningTestData.deleteAll(jdbcTemplate);
        LearningTestData.insertSeries(jdbcTemplate, SERIES, "a-series");
        LearningTestData.insertModule(jdbcTemplate, MODULE, "a-module", SERIES, 1);
        ContentTestData.insertArticle(
                jdbcTemplate, ARTICLE, "joining-article", "Joining article", PublicationStatus.DRAFT);
    }

    @AfterEach
    void clear() {
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void delete_ShouldLeaveNoOrphanMembership_WhenAnArticleIsAddedAtTheSameTime() throws Exception {
        CountDownLatch holderHoldsLock = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);
        CountDownLatch bothStarted = new CountDownLatch(2);
        AtomicInteger adderPid = new AtomicInteger(-1);
        AtomicInteger deleterPid = new AtomicInteger(-1);
        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            Future<Throwable> holder = inTransaction(executor, () -> {
                recordWriter.lockStructure(MODULE, ContentType.MODULE);
                holderHoldsLock.countDown();
                await(releaseHolder);
            });
            await(holderHoldsLock);
            Future<Throwable> adder = inTransaction(executor, () -> {
                adderPid.set(backendPid());
                bothStarted.countDown();
                articleEditor.replace(ARTICLE, 1, new ArticleContentInput(
                        "Joining article", "joining-article", null, "# Body",
                        List.of(), new ArticlePlacement(MODULE, 1)));
            });
            Future<Throwable> deleter = inTransaction(executor, () -> {
                deleterPid.set(backendPid());
                bothStarted.countDown();
                moduleRemover.delete(MODULE, 1);
            });
            await(bothStarted);
            awaitLockWait(adderPid.get());
            awaitLockWait(deleterPid.get());
            releaseHolder.countDown();

            assertThat(holder.get(15, TimeUnit.SECONDS)).isNull();
            Throwable addFailure = adder.get(15, TimeUnit.SECONDS);
            Throwable deleteFailure = deleter.get(15, TimeUnit.SECONDS);

            assertThat(addFailure == null ^ deleteFailure == null).isTrue();
            if (addFailure != null) {
                assertThat(addFailure).isInstanceOfSatisfying(ContentOperationException.class, failure ->
                        assertThat(failure.code()).isEqualTo(ContentFailureCode.MODULE_REFERENCE_NOT_FOUND));
                assertThat(count("module", MODULE)).isZero();
                assertThat(count("module_article", ARTICLE)).isZero();
            } else {
                // The add came first. It raised the revision of the Module, so the old
                // revision of the delete is stale.
                assertThat(deleteFailure).isInstanceOfSatisfying(ContentOperationException.class, failure ->
                        assertThat(failure.code()).isEqualTo(ContentFailureCode.REVISION_CONFLICT));
                assertThat(count("module", MODULE)).isEqualTo(1);
                assertThat(count("module_article", ARTICLE)).isEqualTo(1);
            }
        } finally {
            releaseHolder.countDown();
            executor.shutdownNow();
        }
    }

    private int backendPid() {
        return jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class);
    }

    private long count(String table, UUID id) {
        String column = table.equals("module") ? "content_id" : "article_id";
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE " + column + " = ?", Long.class, id);
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
        throw new IllegalStateException("A writer did not wait for the row lock.");
    }
}
