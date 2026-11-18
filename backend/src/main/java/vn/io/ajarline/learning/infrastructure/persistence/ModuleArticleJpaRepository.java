package vn.io.ajarline.learning.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModuleArticleJpaRepository extends JpaRepository<ModuleArticleEntity, UUID> {

    List<ModuleArticleEntity> findByModuleIdOrderByPositionAsc(UUID moduleId);
}
