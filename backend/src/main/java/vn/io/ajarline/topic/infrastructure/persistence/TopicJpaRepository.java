package vn.io.ajarline.topic.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TopicJpaRepository extends JpaRepository<TopicEntity, UUID> {
}
