package vn.io.ajarline.learning.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ModuleJpaRepository extends JpaRepository<ModuleEntity, UUID> {

    List<ModuleEntity> findBySeriesIdOrderByPositionAsc(UUID seriesId);

    /**
     * Reads the Modules of many Series with one query. They come grouped by Series,
     * and each group is in position order.
     */
    List<ModuleEntity> findBySeriesIdInOrderBySeriesIdAscPositionAsc(Collection<UUID> seriesIds);
}
