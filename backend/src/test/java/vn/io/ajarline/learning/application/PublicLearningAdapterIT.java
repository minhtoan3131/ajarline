package vn.io.ajarline.learning.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import vn.io.ajarline.content.contract.PublicArticleContext;
import vn.io.ajarline.content.contract.PublicContentContext;
import vn.io.ajarline.content.contract.PublicLearningContextPort;
import vn.io.ajarline.learning.contract.LearningVisibility;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.io.ajarline.support.LearningTestData.ARTICLE_A1;
import static vn.io.ajarline.support.LearningTestData.ARTICLE_A3;
import static vn.io.ajarline.support.LearningTestData.ARTICLE_A4;
import static vn.io.ajarline.support.LearningTestData.ARTICLE_A5;
import static vn.io.ajarline.support.LearningTestData.ARTICLE_C1;
import static vn.io.ajarline.support.LearningTestData.ARTICLE_E;
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

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
class PublicLearningAdapterIT {

    @Autowired
    private LearningVisibility visibility;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private PublicLearningContextPort port;

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
    void publicModuleIds_ShouldReturnOnlyPublicModules_WhenEverySituationExists() {
        List<UUID> everyModule = List.of(MODULE_A1, MODULE_A2, MODULE_A3, MODULE_B1, MODULE_C1, MODULE_D1);

        Set<UUID> result = transactions.execute(status -> visibility.publicModuleIds(everyModule));

        assertThat(result).containsExactlyInAnyOrder(MODULE_A1, MODULE_A2, MODULE_C1);
    }

    @Test
    void publicModuleIds_ShouldReturnNothing_WhenNoIdIsGiven() {
        Set<UUID> result = transactions.execute(status -> visibility.publicModuleIds(List.of()));

        assertThat(result).isEmpty();
    }

    @Test
    void publicAmong_ShouldReturnPublicSeriesAndModules_WhenBothKindsAreGiven() {
        List<UUID> ids = List.of(SERIES_A, SERIES_B, SERIES_C, SERIES_D,
                MODULE_A1, MODULE_A2, MODULE_A3, MODULE_B1, MODULE_C1, MODULE_D1);

        Set<UUID> result = transactions.execute(status -> port.publicAmong(ids));

        assertThat(result).containsExactlyInAnyOrder(SERIES_A, MODULE_A1, MODULE_A2, MODULE_C1);
    }

    @Test
    void contextsOf_ShouldGivePublicParentsOnly_WhenEverySituationExists() {
        List<UUID> ids = List.of(ARTICLE_A3, ARTICLE_A4, ARTICLE_C1, ARTICLE_A5, ARTICLE_E, MODULE_A2, MODULE_C1);

        Map<UUID, PublicContentContext> contexts = transactions.execute(status -> port.contextsOf(ids));

        assertThat(contexts).containsOnlyKeys(ids);
        PublicContentContext inFirstModule = contexts.get(ARTICLE_A3);
        assertThat(inFirstModule.module().slug()).isEqualTo("module-a1");
        assertThat(inFirstModule.module().position()).isEqualTo(1);
        assertThat(inFirstModule.series().slug()).isEqualTo("series-a");
        // Module A3 is hidden between A1 and A2, so A2 has rank 2.
        assertThat(contexts.get(ARTICLE_A4).module().position()).isEqualTo(2);
        PublicContentContext underHiddenSeries = contexts.get(ARTICLE_C1);
        assertThat(underHiddenSeries.module().slug()).isEqualTo("module-c1");
        assertThat(underHiddenSeries.module().position()).isEqualTo(1);
        assertThat(underHiddenSeries.series()).isNull();
        assertThat(contexts.get(ARTICLE_A5)).isEqualTo(PublicContentContext.EMPTY);
        assertThat(contexts.get(ARTICLE_E)).isEqualTo(PublicContentContext.EMPTY);
        assertThat(contexts.get(MODULE_A2).series().slug()).isEqualTo("series-a");
        assertThat(contexts.get(MODULE_A2).module()).isNull();
        assertThat(contexts.get(MODULE_C1)).isEqualTo(PublicContentContext.EMPTY);
    }

    @Test
    void publicAmong_ShouldFail_WhenCalledOutsideTransaction() {
        assertThatThrownBy(() -> port.publicAmong(List.of(SERIES_A)))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void contextsOf_ShouldFail_WhenCalledOutsideTransaction() {
        assertThatThrownBy(() -> port.contextsOf(List.of(ARTICLE_A3)))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void publicModuleIds_ShouldFail_WhenCalledOutsideTransaction() {
        assertThatThrownBy(() -> visibility.publicModuleIds(List.of(MODULE_A1)))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void articleContextOf_ShouldGiveParentsAndNeighbours_WhenArticleIsInPublicModule() {
        PublicArticleContext context = transactions.execute(status -> port.articleContextOf(ARTICLE_A3));

        assertThat(context.context().module().slug()).isEqualTo("module-a1");
        assertThat(context.context().module().position()).isEqualTo(1);
        assertThat(context.context().series().slug()).isEqualTo("series-a");
        // Article A2 is a draft between A1 and A3, so the nearest public Article before A3 is A1.
        assertThat(context.previous().slug()).isEqualTo("article-a1");
        assertThat(context.previous().position()).isEqualTo(1);
        assertThat(context.next()).isNull();
    }

    @Test
    void articleContextOf_ShouldBeEmpty_WhenModuleIsHidden() {
        PublicArticleContext context = transactions.execute(status -> port.articleContextOf(ARTICLE_A5));

        assertThat(context).isEqualTo(PublicArticleContext.EMPTY);
    }

    @Test
    void articleContextOf_ShouldHaveNoSeries_WhenOnlyTheSeriesIsHidden() {
        PublicArticleContext context = transactions.execute(status -> port.articleContextOf(ARTICLE_C1));

        assertThat(context.context().module().slug()).isEqualTo("module-c1");
        assertThat(context.context().series()).isNull();
    }

    @Test
    void articleIdsInPublicModules_ShouldReturnPublicArticlesOfPublicModules_WhenSomeModulesAreHidden() {
        Set<UUID> ids = transactions.execute(status -> port.articleIdsInPublicModules());

        assertThat(ids).containsExactlyInAnyOrder(ARTICLE_A1, ARTICLE_A3, ARTICLE_A4, ARTICLE_C1);
    }

    @Test
    void articleContextOf_ShouldFail_WhenCalledOutsideTransaction() {
        assertThatThrownBy(() -> port.articleContextOf(ARTICLE_A3))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void articleIdsInPublicModules_ShouldFail_WhenCalledOutsideTransaction() {
        assertThatThrownBy(() -> port.articleIdsInPublicModules())
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
