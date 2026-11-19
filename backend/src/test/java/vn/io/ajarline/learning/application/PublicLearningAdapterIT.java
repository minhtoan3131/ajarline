package vn.io.ajarline.learning.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import vn.io.ajarline.learning.contract.LearningVisibility;
import vn.io.ajarline.support.LearningTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.support.LearningTestData.MODULE_A1;
import static vn.io.ajarline.support.LearningTestData.MODULE_A2;
import static vn.io.ajarline.support.LearningTestData.MODULE_A3;
import static vn.io.ajarline.support.LearningTestData.MODULE_B1;
import static vn.io.ajarline.support.LearningTestData.MODULE_C1;
import static vn.io.ajarline.support.LearningTestData.MODULE_D1;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
class PublicLearningAdapterIT {

    @Autowired
    private LearningVisibility visibility;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactions;

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
}
