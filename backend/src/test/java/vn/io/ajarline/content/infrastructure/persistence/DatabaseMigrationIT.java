package vn.io.ajarline.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
public class DatabaseMigrationIT {
    @Autowired
    private Flyway flyway;

    @Test
    void migration_ShouldReachVersionOne_WhenDatabaseStartsEmpty() {
        MigrationInfo current = flyway.info().current();
        assertThat(current).isNotNull();
        assertThat(current.getVersion().getVersion()).isEqualTo("1");
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void migration_ShouldRemainCurrent_WhenMigrationRunsAgain() {
        assertThat(flyway.info().pending()).isEmpty();
        flyway.migrate();
        assertThat(flyway.info().pending()).isEmpty();
        assertThatCode(flyway::validate).doesNotThrowAnyException();
    }
}
