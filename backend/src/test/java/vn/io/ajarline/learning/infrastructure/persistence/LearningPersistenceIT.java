package vn.io.ajarline.learning.infrastructure.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.support.ContentTestData;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.TopicTestData.uuid;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
class LearningPersistenceIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SeriesJpaRepository seriesRepository;

    @Autowired
    private ModuleJpaRepository moduleRepository;

    @Autowired
    private ModuleArticleJpaRepository moduleArticleRepository;

    @BeforeEach
    void removeRowsBeforeTest() {
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @AfterEach
    void removeRowsAfterTest() {
        LearningTestData.deleteAll(jdbcTemplate);
    }

    @Test
    void findById_ShouldReturnOverview_WhenSeriesExists() {
        LearningTestData.insertSeries(
                jdbcTemplate, uuid(1), "a-series", PublicationStatus.DRAFT, "# Overview");

        SeriesEntity series = seriesRepository.findById(uuid(1)).orElseThrow();

        assertThat(series.getContentId()).isEqualTo(uuid(1));
        assertThat(series.getOverviewMarkdown()).isEqualTo("# Overview");
    }

    @Test
    void findBySeriesIdOrderByPositionAsc_ShouldReturnModulesInOrder_WhenRowsWereInsertedOutOfOrder() {
        UUID seriesId = uuid(1);
        LearningTestData.insertSeries(jdbcTemplate, seriesId, "a-series");
        LearningTestData.insertModule(jdbcTemplate, uuid(3), "second-module", seriesId, 2);
        LearningTestData.insertModule(jdbcTemplate, uuid(2), "first-module", seriesId, 1);

        List<ModuleEntity> modules = moduleRepository.findBySeriesIdOrderByPositionAsc(seriesId);

        assertThat(modules).extracting(ModuleEntity::getContentId)
                .containsExactly(uuid(2), uuid(3));
        assertThat(modules).extracting(ModuleEntity::getPosition).containsExactly(1, 2);
        assertThat(modules).extracting(ModuleEntity::getSeriesId)
                .containsOnly(seriesId);
    }

    @Test
    void findByModuleIdOrderByPositionAsc_ShouldReturnMembershipsInOrder_WhenRowsWereInsertedOutOfOrder() {
        UUID moduleId = uuid(2);
        LearningTestData.insertSeries(jdbcTemplate, uuid(1), "a-series");
        LearningTestData.insertModule(jdbcTemplate, moduleId, "a-module", uuid(1), 1);
        ContentTestData.insertArticle(
                jdbcTemplate, uuid(3), "first-article", "First", PublicationStatus.DRAFT);
        ContentTestData.insertArticle(
                jdbcTemplate, uuid(4), "second-article", "Second", PublicationStatus.DRAFT);
        LearningTestData.insertModuleArticle(jdbcTemplate, uuid(4), moduleId, 2);
        LearningTestData.insertModuleArticle(jdbcTemplate, uuid(3), moduleId, 1);

        List<ModuleArticleEntity> memberships =
                moduleArticleRepository.findByModuleIdOrderByPositionAsc(moduleId);

        assertThat(memberships).extracting(ModuleArticleEntity::getArticleId)
                .containsExactly(uuid(3), uuid(4));
        assertThat(memberships).extracting(ModuleArticleEntity::getPosition)
                .containsExactly(1, 2);
    }
}
