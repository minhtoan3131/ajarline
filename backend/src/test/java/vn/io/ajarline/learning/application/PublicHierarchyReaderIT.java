package vn.io.ajarline.learning.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.LearningTestData.ARTICLE_A1;
import static vn.io.ajarline.support.LearningTestData.ARTICLE_A3;
import static vn.io.ajarline.support.LearningTestData.MODULE_A1;
import static vn.io.ajarline.support.LearningTestData.MODULE_A2;
import static vn.io.ajarline.support.LearningTestData.MODULE_A3;
import static vn.io.ajarline.support.LearningTestData.MODULE_B1;
import static vn.io.ajarline.support.LearningTestData.MODULE_C1;
import static vn.io.ajarline.support.LearningTestData.MODULE_D1;
import static vn.io.ajarline.support.LearningTestData.SERIES_A;
import static vn.io.ajarline.support.LearningTestData.SERIES_B;
import static vn.io.ajarline.support.LearningTestData.SERIES_C;
import static vn.io.ajarline.support.LearningTestData.SERIES_D;
import static vn.io.ajarline.support.TopicTestData.uuid;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
class PublicHierarchyReaderIT {

    @Autowired
    private PublicHierarchyReader reader;

    @Autowired
    private PublicHierarchyPolicy policy;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void insertScenarios() {
        LearningTestData.deleteAll(jdbcTemplate);
        LearningTestData.insertStandardScenarios(jdbcTemplate);
    }

    @AfterEach
    void removeRows() {
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @Test
    void readDown_ShouldKeepStoredOrderAndSkipHiddenChildren_WhenSeriesIsAsked() {
        PublicHierarchy hierarchy = judgeDown(SERIES_A);

        assertThat(hierarchy.isPublic(SERIES_A)).isTrue();
        assertThat(hierarchy.publicChildrenOf(SERIES_A)).containsExactly(MODULE_A1, MODULE_A2);
        assertThat(hierarchy.publicChildrenOf(MODULE_A1)).containsExactly(ARTICLE_A1, ARTICLE_A3);
        assertThat(hierarchy.isPublic(MODULE_A3)).isFalse();
    }

    @Test
    void readDown_ShouldDecideEverySeries_WhenManySeriesAreAskedAtOnce() {
        PublicHierarchy hierarchy = judgeDown(SERIES_A, SERIES_B, SERIES_C, SERIES_D);

        assertThat(hierarchy.publicIds()).containsExactlyInAnyOrder(SERIES_A, MODULE_A1, MODULE_A2);
    }

    @Test
    void readDown_ShouldDecideEveryModule_WhenModulesAreAskedWithoutTheirSeries() {
        PublicHierarchy hierarchy = judgeDown(MODULE_A1, MODULE_A3, MODULE_B1, MODULE_C1, MODULE_D1);

        assertThat(hierarchy.publicIds()).containsExactlyInAnyOrder(MODULE_A1, MODULE_C1);
    }

    @Test
    void readDown_ShouldIgnoreTheId_WhenNoItemHasIt() {
        PublicHierarchy hierarchy = judgeDown(uuid(9999), SERIES_A);

        assertThat(hierarchy.isPublic(SERIES_A)).isTrue();
        assertThat(hierarchy.isPublic(uuid(9999))).isFalse();
    }

    @Test
    void readDown_ShouldNotWait_WhenAnotherTransactionLocksTheRows() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        TransactionStatus adminTransaction = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            jdbcTemplate.queryForList(
                    "SELECT id FROM content_item WHERE id IN (?, ?) FOR UPDATE",
                    UUID.class, SERIES_A, MODULE_A1);

            Future<PublicHierarchy> reading = executor.submit(() -> judgeDown(SERIES_A));

            assertThat(reading.get(5, TimeUnit.SECONDS).isPublic(SERIES_A)).isTrue();
        } finally {
            transactionManager.rollback(adminTransaction);
            executor.shutdownNow();
        }
    }

    private PublicHierarchy judgeDown(UUID... ids) {
        return transactions.execute(status -> policy.judge(reader.readDown(List.of(ids))));
    }
}
