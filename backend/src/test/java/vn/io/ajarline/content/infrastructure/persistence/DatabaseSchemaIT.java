package vn.io.ajarline.content.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
public class DatabaseSchemaIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void schema_ShouldContainOnlyCurrentTable_WhenMigrationCompletes() {
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """, String.class);
        assertThat(tables).containsExactlyInAnyOrder(
                "flyway_schema_history",
                "content_item",
                "article"
        );
    }

    @Test
    void schema_ShouldContainOnlyConstraintIndexes_WhenMigrationCompletes() {
        List<String> indexes = jdbcTemplate.queryForList("""
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                AND tablename IN ('content_item', 'article')
                ORDER BY indexname
                """, String.class);
        assertThat(indexes).containsExactlyInAnyOrder(
                "pk_content_item",
                "uk_content_item_type_slug",
                "pk_article"
        );
    }

    @Test
    void bodyMarkdown_ShouldUsePostgreSqlText_WhenMigrationCompletes() {
        String dataType = jdbcTemplate.queryForObject("""
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = 'public'
                AND table_name = 'article'
                AND column_name = 'body_markdown'
                """, String.class);
        assertThat(dataType).isEqualTo("text");
    }
}
