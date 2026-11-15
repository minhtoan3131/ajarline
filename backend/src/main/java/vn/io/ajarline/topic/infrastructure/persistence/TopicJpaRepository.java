package vn.io.ajarline.topic.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TopicJpaRepository extends JpaRepository<TopicEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select topic from TopicEntity topic where topic.id = :id")
    Optional<TopicEntity> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Tells whether any content item is assigned to the Topic. The result does
     * not depend on the publication state of the content.
     */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM content_topic WHERE topic_id = :topicId)",
            nativeQuery = true)
    boolean isAssigned(@Param("topicId") UUID topicId);
}
