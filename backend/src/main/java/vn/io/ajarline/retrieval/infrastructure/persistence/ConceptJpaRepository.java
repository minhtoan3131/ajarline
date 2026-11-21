package vn.io.ajarline.retrieval.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConceptJpaRepository extends JpaRepository<ConceptEntity, UUID> {

    /**
     * Returns every Concept with its expressions, ordered by display name and then by id.
     */
    @EntityGraph(attributePaths = "expressions")
    @Query("select concept from ConceptEntity concept order by concept.displayName, concept.id")
    List<ConceptEntity> findAllWithExpressions();

    /**
     * Locks the Concept row. The expressions load later, after the lock is held.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select concept from ConceptEntity concept where concept.id = :id")
    Optional<ConceptEntity> findByIdForUpdate(@Param("id") UUID id);
}
