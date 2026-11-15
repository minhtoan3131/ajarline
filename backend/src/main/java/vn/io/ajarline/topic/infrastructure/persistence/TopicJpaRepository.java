package vn.io.ajarline.topic.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicJpaRepository extends JpaRepository<TopicEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select topic from TopicEntity topic where topic.id = :id")
    Optional<TopicEntity> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Locks the given Topics for sharing, in id order. A Topic that another
     * transaction is changing or deleting makes this call wait, so the
     * returned rows are the ones that really exist.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select topic from TopicEntity topic where topic.id in :ids order by topic.id")
    List<TopicEntity> findAllByIdForShare(@Param("ids") Collection<UUID> ids);

    /**
     * Tells whether any content item is assigned to the Topic. The result does
     * not depend on the publication state of the content.
     */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM content_topic WHERE topic_id = :topicId)",
            nativeQuery = true)
    boolean isAssigned(@Param("topicId") UUID topicId);

    /** Returns the content ids that carry the Topic, whatever their publication state. */
    @Query(value = "SELECT content_id FROM content_topic WHERE topic_id = :topicId ORDER BY content_id",
            nativeQuery = true)
    List<UUID> findAssignedContentIds(@Param("topicId") UUID topicId);

    /** Returns the Topic ids assigned to a content item, ordered by id. */
    @Query(value = "SELECT topic_id FROM content_topic WHERE content_id = :contentId ORDER BY topic_id",
            nativeQuery = true)
    List<UUID> findAssignedTopicIds(@Param("contentId") UUID contentId);

    @Modifying
    @Query(value = "INSERT INTO content_topic(content_id, topic_id) VALUES (:contentId, :topicId)",
            nativeQuery = true)
    void insertAssignment(@Param("contentId") UUID contentId, @Param("topicId") UUID topicId);

    @Modifying
    @Query(value = "DELETE FROM content_topic WHERE content_id = :contentId AND topic_id IN (:topicIds)",
            nativeQuery = true)
    void deleteAssignments(@Param("contentId") UUID contentId, @Param("topicIds") Collection<UUID> topicIds);

    /**
     * Returns the Topics assigned to the given content items, one row per
     * assignment, ordered by Topic name and then by Topic id.
     */
    @Query(value = """
            SELECT assignment.content_id AS "contentId",
                   t.id                  AS "id",
                   t.slug                AS "slug",
                   t.name                AS "name"
              FROM content_topic assignment
              JOIN topic t ON t.id = assignment.topic_id
             WHERE assignment.content_id IN (:contentIds)
             ORDER BY t.name, t.id
            """, nativeQuery = true)
    List<TopicLabelRow> findLabelsOfContent(@Param("contentIds") Collection<UUID> contentIds);
}
