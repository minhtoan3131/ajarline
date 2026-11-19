package vn.io.ajarline.learning.infrastructure.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.ContentTestData;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.io.ajarline.support.TopicTestData.uuid;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
class LearningSchemaConstraintIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void removeRowsBeforeTest() {
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @AfterEach
    void removeRowsAfterTest() {
        // Other test classes clean with a plain DELETE FROM content_item, which
        // is not safe when hierarchy rows are left behind.
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @Test
    void insert_ShouldAcceptSeriesAndModuleTypes_WhenTypeConstraintIsUpdated() {
        UUID seriesId = uuid(1);
        LearningTestData.insertSeries(jdbcTemplate, seriesId, "a-series");
        LearningTestData.insertModule(jdbcTemplate, uuid(2), "a-module", seriesId, 1);

        assertThat(rowCount("content_item")).isEqualTo(2);
        assertThat(rowCount("series")).isEqualTo(1);
        assertThat(rowCount("module")).isEqualTo(1);
    }

    @Test
    void insert_ShouldRejectModule_WhenSeriesIdIsNull() {
        UUID moduleId = uuid(2);
        LearningTestData.insertContentItem(
                jdbcTemplate, moduleId, "MODULE", "orphan-module", PublicationStatus.DRAFT);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO module(content_id, series_id, position, overview_markdown)
                VALUES (?, NULL, 1, NULL)
                """, moduleId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("series_id");
    }

    @Test
    void insert_ShouldRejectModule_WhenPositionIsBelowOne() {
        UUID seriesId = uuid(1);
        LearningTestData.insertSeries(jdbcTemplate, seriesId, "a-series");

        assertThatThrownBy(() -> LearningTestData.insertModule(
                jdbcTemplate, uuid(2), "a-module", seriesId, 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_module_position");
    }

    @Test
    void delete_ShouldRejectSeries_WhenAModuleStillBelongsToIt() {
        UUID seriesId = uuid(1);
        LearningTestData.insertSeries(jdbcTemplate, seriesId, "a-series");
        LearningTestData.insertModule(jdbcTemplate, uuid(2), "a-module", seriesId, 1);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM series WHERE content_id = ?", seriesId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_module_series_id");
    }

    @Test
    void delete_ShouldRejectModule_WhenAnArticleStillBelongsToIt() {
        UUID seriesId = uuid(1);
        UUID moduleId = uuid(2);
        LearningTestData.insertSeries(jdbcTemplate, seriesId, "a-series");
        LearningTestData.insertModule(jdbcTemplate, moduleId, "a-module", seriesId, 1);
        ContentTestData.insertArticle(
                jdbcTemplate, uuid(3), "an-article", "An article", PublicationStatus.DRAFT);
        LearningTestData.insertModuleArticle(jdbcTemplate, uuid(3), moduleId, 1);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM module WHERE content_id = ?", moduleId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_module_article_module_id");
    }

    @Test
    void insert_ShouldRejectSecondMembership_WhenArticleIsAlreadyInAModule() {
        UUID seriesId = uuid(1);
        UUID articleId = uuid(4);
        LearningTestData.insertSeries(jdbcTemplate, seriesId, "a-series");
        LearningTestData.insertModule(jdbcTemplate, uuid(2), "first-module", seriesId, 1);
        LearningTestData.insertModule(jdbcTemplate, uuid(3), "second-module", seriesId, 2);
        ContentTestData.insertArticle(
                jdbcTemplate, articleId, "an-article", "An article", PublicationStatus.DRAFT);
        LearningTestData.insertModuleArticle(jdbcTemplate, articleId, uuid(2), 1);

        assertThatThrownBy(() -> LearningTestData.insertModuleArticle(
                jdbcTemplate, articleId, uuid(3), 1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("pk_module_article");
    }

    @Test
    void insert_ShouldNotCheckPositionImmediately_WhenTwoModulesShareAPosition() {
        UUID seriesId = uuid(1);
        LearningTestData.insertSeries(jdbcTemplate, seriesId, "a-series");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        // No exception: the unique check is waiting for commit, so a service
        // can shift several siblings with several statements.
        assertThatCode(() -> transaction.executeWithoutResult(status -> {
            LearningTestData.insertModule(jdbcTemplate, uuid(2), "first-module", seriesId, 1);
            LearningTestData.insertModule(jdbcTemplate, uuid(3), "second-module", seriesId, 1);
            status.setRollbackOnly();
        })).doesNotThrowAnyException();
        assertThat(rowCount("module")).isZero();
    }

    @Test
    void commit_ShouldRejectDuplicatePosition_WhenTwoModulesShareAPositionInOneSeries() {
        UUID seriesId = uuid(1);
        LearningTestData.insertSeries(jdbcTemplate, seriesId, "a-series");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        // The failure surfaces from commit, so the wrapper exception type depends
        // on the transaction manager. The constraint name is the stable part.
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            LearningTestData.insertModule(jdbcTemplate, uuid(2), "first-module", seriesId, 1);
            LearningTestData.insertModule(jdbcTemplate, uuid(3), "second-module", seriesId, 1);
        })).hasStackTraceContaining("uk_module_series_position");
        assertThat(rowCount("module")).isZero();
    }

    @Test
    void commit_ShouldRejectDuplicatePosition_WhenTwoArticlesShareAPositionInOneModule() {
        UUID seriesId = uuid(1);
        UUID moduleId = uuid(2);
        LearningTestData.insertSeries(jdbcTemplate, seriesId, "a-series");
        LearningTestData.insertModule(jdbcTemplate, moduleId, "a-module", seriesId, 1);
        ContentTestData.insertArticle(
                jdbcTemplate, uuid(3), "first-article", "First", PublicationStatus.DRAFT);
        ContentTestData.insertArticle(
                jdbcTemplate, uuid(4), "second-article", "Second", PublicationStatus.DRAFT);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            LearningTestData.insertModuleArticle(jdbcTemplate, uuid(3), moduleId, 1);
            LearningTestData.insertModuleArticle(jdbcTemplate, uuid(4), moduleId, 1);
        })).hasStackTraceContaining("uk_module_article_module_position");
        assertThat(rowCount("module_article")).isZero();
    }

    private long rowCount(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
