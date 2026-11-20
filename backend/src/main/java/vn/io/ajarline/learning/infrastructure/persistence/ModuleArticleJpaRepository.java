package vn.io.ajarline.learning.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleArticleJpaRepository extends JpaRepository<ModuleArticleEntity, UUID> {

    List<ModuleArticleEntity> findByModuleIdOrderByPositionAsc(UUID moduleId);

    /**
     * Reads the memberships of many Modules with one query. They come grouped by Module,
     * and each group is in position order.
     */
    List<ModuleArticleEntity> findByModuleIdInOrderByModuleIdAscPositionAsc(Collection<UUID> moduleIds);

    /**
     * Returns the Module of an Article without loading the entity. Then no copy of the row
     * stays in the persistence context before the Module is locked.
     */
    @Query("""
            select membership.moduleId from ModuleArticleEntity membership
             where membership.articleId = :articleId
            """)
    Optional<UUID> findModuleIdByArticleId(@Param("articleId") UUID articleId);

    /**
     * Tells whether the Module has at least one Article.
     */
    boolean existsByModuleId(UUID moduleId);
}
