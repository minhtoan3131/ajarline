package vn.io.ajarline.learning.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SeriesJpaRepository extends JpaRepository<SeriesEntity, UUID> {

    /**
     * Returns the id of every Series.
     */
    @Query("select seriesRow.contentId from SeriesEntity seriesRow")
    List<UUID> findAllContentIds();
}
