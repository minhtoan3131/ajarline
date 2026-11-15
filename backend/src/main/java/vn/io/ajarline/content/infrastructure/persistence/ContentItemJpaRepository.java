package vn.io.ajarline.content.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicationStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentItemJpaRepository extends JpaRepository<ContentItemEntity, UUID> {

    Optional<ContentItemEntity> findByTypeAndSlugAndStatus(
            ContentType type,
            String slug,
            PublicationStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from ContentItemEntity item where item.id = :id")
    Optional<ContentItemEntity> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Advances the revision of the listed items in one statement. The
     * statement takes row locks, so a concurrent Article save waits for it.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ContentItemEntity item
               set item.revision = item.revision + 1,
                   item.updatedAt = :changedAt
             where item.id in :ids
            """)
    int advanceRevisions(@Param("ids") Collection<UUID> ids, @Param("changedAt") Instant changedAt);

    /**
     * Returns the ids, among the given ones, of the items in the given status.
     */
    @Query("""
            select item.id from ContentItemEntity item
             where item.id in :ids
               and item.status = :status
            """)
    List<UUID> findIdsWithStatus(
            @Param("ids") Collection<UUID> ids,
            @Param("status") PublicationStatus status);

    /**
     * Returns one window of items in the given status, in the public browse
     * order described by PublicContentQuery#findPublicPage. The type rank is
     * written for every public type; the database decides which types can
     * exist.
     */
    @Query(value = """
            SELECT * FROM content_item item
             WHERE item.id IN (:ids)
               AND item.status = :status
               AND item.type IN (:types)
             ORDER BY CASE item.type
                          WHEN 'SERIES' THEN 0
                          WHEN 'MODULE' THEN 1
                          WHEN 'ARTICLE' THEN 2
                          ELSE 3
                      END,
                      CASE WHEN item.type = 'NEWS' THEN NULL ELSE item.title END,
                      CASE WHEN item.type = 'NEWS' THEN item.published_at END DESC,
                      item.id
             LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<ContentItemEntity> findWindowWithStatus(
            @Param("ids") Collection<UUID> ids,
            @Param("status") String status,
            @Param("types") Collection<String> types,
            @Param("limit") int limit,
            @Param("offset") long offset);
}
