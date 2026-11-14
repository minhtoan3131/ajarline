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
}
