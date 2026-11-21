package vn.io.ajarline.retrieval.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface VocabularyStateJpaRepository extends JpaRepository<VocabularyStateEntity, Short> {

    /**
     * Adds one to the version in a single statement and returns the number of changed rows.
     */
    @Modifying
    @Query(value = """
            UPDATE retrieval_vocabulary_state
               SET version = version + 1, updated_at = :now
             WHERE id = 1
            """, nativeQuery = true)
    int advance(@Param("now") Instant now);
}
