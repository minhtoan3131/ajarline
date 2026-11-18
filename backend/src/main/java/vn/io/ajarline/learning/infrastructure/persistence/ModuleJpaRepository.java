package vn.io.ajarline.learning.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModuleJpaRepository extends JpaRepository<ModuleEntity, UUID> {

    List<ModuleEntity> findBySeriesIdOrderByPositionAsc(UUID seriesId);
}
