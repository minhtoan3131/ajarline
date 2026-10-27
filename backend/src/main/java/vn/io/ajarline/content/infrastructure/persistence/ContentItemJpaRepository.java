package vn.io.ajarline.content.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicationStatus;

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
}
