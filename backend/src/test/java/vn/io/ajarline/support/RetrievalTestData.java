package vn.io.ajarline.support;

import org.springframework.jdbc.core.JdbcTemplate;
import vn.io.ajarline.retrieval.application.EditableConcept;
import vn.io.ajarline.retrieval.application.ExpressionDraft;
import vn.io.ajarline.retrieval.application.ExpressionNormalizer;
import vn.io.ajarline.retrieval.application.NewConcept;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Builds vocabulary values and inserts rows directly through SQL, so tests can build
 * database states that the application services would refuse to create.
 */
public final class RetrievalTestData {

    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.parse("2026-11-21T00:00:00Z");

    private RetrievalTestData() {
    }

    public static UUID uuid(int suffix) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(suffix));
    }

    public static void insertConcept(
            JdbcTemplate jdbcTemplate, UUID id, String key, String displayName, Boolean active) {
        jdbcTemplate.update("""
                INSERT INTO retrieval_concept(id, key, display_name, description, active,
                    created_at, updated_at)
                VALUES (?, ?, ?, NULL, ?, ?, ?)
                """, id, key, displayName, active, FIXED_TIME, FIXED_TIME);
    }

    public static void insertExpression(
            JdbcTemplate jdbcTemplate, UUID id, UUID conceptId, String expression,
            String normalizedExpression, String languageHint, Boolean autoExpand) {
        jdbcTemplate.update("""
                INSERT INTO retrieval_concept_expression(id, concept_id, expression,
                    normalized_expression, language_hint, auto_expand, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, conceptId, expression, normalizedExpression, languageHint, autoExpand,
                FIXED_TIME, FIXED_TIME);
    }

    /** Deletes every Concept. The expressions go with them. */
    public static void deleteAllConcepts(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM retrieval_concept");
    }

    public static long currentVersion(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject(
                "SELECT version FROM retrieval_vocabulary_state WHERE id = 1", Long.class);
    }

    /** Builds an expression the way the policy does: the normalized text comes from the real function. */
    public static ExpressionDraft draft(String expression, String languageHint, boolean autoExpand) {
        return new ExpressionDraft(
                expression, ExpressionNormalizer.normalize(expression), languageHint, autoExpand);
    }

    public static EditableConcept editable(
            String displayName, boolean active, ExpressionDraft... drafts) {
        return new EditableConcept(displayName, null, active, List.of(drafts));
    }

    public static NewConcept newConcept(
            String key, String displayName, boolean active, ExpressionDraft... drafts) {
        return new NewConcept(key, editable(displayName, active, drafts));
    }
}
