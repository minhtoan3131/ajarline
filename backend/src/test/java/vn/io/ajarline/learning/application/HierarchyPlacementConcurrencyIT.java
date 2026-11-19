package vn.io.ajarline.learning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.TopicTestData.uuid;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
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
import vn.io.ajarline.content.contract.ContentRecordInput;
import vn.io.ajarline.content.contract.ContentRecordWriter;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.learning.contract.ModuleContentInput;
import vn.io.ajarline.learning.contract.ModuleEditor;
import vn.io.ajarline.support.ContentApplicationTestConfiguration;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest
@Import({
        PostgreSqlTestConfiguration.class,
        ContentApplicationTestConfiguration.class
})
class HierarchyPlacementConcurrencyIT {

    private static final UUID SERIES = uuid(1);
    private static final UUID TARGET_ONE = uuid(2);
    private static final UUID TARGET_TWO = uuid(3);
    private static final UUID MODULE_1 = uuid(11);
    private static final UUID MODULE_2 = uuid(12);
    private static final UUID MODULE_3 = uuid(13);
    private static final UUID MODULE_4 = uuid(14);

    @Autowired
    private ModuleEditor moduleEditor;

    @Autowired
    private ContentRecordWriter recordWriter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void insertSeriesWithFourModules() {
        LearningTestData.deleteAll(jdbcTemplate);
        LearningTestData.insertSeries(jdbcTemplate, SERIES, "source-series");
        LearningTestData.insertSeries(jdbcTemplate, TARGET_ONE, "target-one");
        LearningTestData.insertSeries(jdbcTemplate, TARGET_TWO, "target-two");
        LearningTestData.insertModule(jdbcTemplate, MODULE_1, "module-1", SERIES, 1);
        LearningTestData.insertModule(jdbcTemplate, MODULE_2, "module-2", SERIES, 2);
        LearningTestData.insertModule(jdbcTemplate, MODULE_3, "module-3", SERIES, 3);
        LearningTestData.insertModule(jdbcTemplate, MODULE_4, "module-4", SERIES, 4);
    }

    @AfterEach
    void removeRows() {
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void replace_ShouldKeepPositionsContinuous_WhenTwoModulesLeaveTheSameSeriesAtOnce()
            throws Exception {
        CountDownLatch seriesLocked = new CountDownLatch(1);
        CountDownLatch contenderStarted = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);
        AtomicInteger contenderBackendPid = new AtomicInteger(-1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // The holder locks the Series like the placement step does, then waits.
            // It moves Module 3 only after the contender is blocked.
            Future<?> holder = executor.submit(inTransaction(() -> {
                recordWriter.lockStructure(SERIES, ContentType.SERIES);
                seriesLocked.countDown();
                await(releaseHolder);
                moveTo(MODULE_3, "module-3", TARGET_ONE);
            }));
            await(seriesLocked);

            Future<?> contender = executor.submit(inTransaction(() -> {
                contenderBackendPid.set(
                        jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class));
                contenderStarted.countDown();
                moveTo(MODULE_1, "module-1", TARGET_TWO);
            }));
            await(contenderStarted);

            awaitPostgreSqlLockWait(contenderBackendPid.get());
            releaseHolder.countDown();
            holder.get(15, TimeUnit.SECONDS);
            contender.get(15, TimeUnit.SECONDS);
        } finally {
            releaseHolder.countDown();
            executor.shutdownNow();
        }

        List<Map<String, Object>> remaining = jdbcTemplate.queryForList("""
                SELECT content_id, position FROM module
                WHERE series_id = ? ORDER BY position
                """, SERIES);
        assertThat(remaining).extracting(row -> row.get("content_id"))
                .containsExactly(MODULE_2, MODULE_4);
        assertThat(remaining).extracting(row -> row.get("position"))
                .containsExactly(1, 2);
        // Each move raised the Series and Module 4 once.
        assertThat(revisionOf(SERIES)).isEqualTo(3);
        assertThat(revisionOf(MODULE_4)).isEqualTo(3);
    }

    private void moveTo(UUID moduleId, String slug, UUID seriesId) {
        moduleEditor.replace(moduleId, 1, new ModuleContentInput(
                new ContentRecordInput("Fixture " + slug, slug, "Fixture summary", List.of()),
                "Fixture overview",
                seriesId,
                1));
    }

    /**
     * Runs the body in one real transaction. A failure reaches the test through {@code Future.get}.
     */
    private Callable<Object> inTransaction(Runnable body) {
        return () -> {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.setTimeout(20);
            transaction.executeWithoutResult(status -> body.run());
            return null;
        };
    }

    private long revisionOf(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT revision FROM content_item WHERE id = ?", Long.class, id);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The test did not reach an expected step.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrency test was interrupted.", exception);
        }
    }

    /**
     * Waits until PostgreSQL reports a lock wait. This proves the contender is blocked, not just slow.
     */
    private void awaitPostgreSqlLockWait(int backendPid) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            Boolean waiting = jdbcTemplate.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1 FROM pg_stat_activity
                        WHERE pid = ? AND wait_event_type = 'Lock'
                    )
                    """, Boolean.class, backendPid);
            if (Boolean.TRUE.equals(waiting)) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
        }
        throw new IllegalStateException("The contender did not wait for the Series lock.");
    }
}
