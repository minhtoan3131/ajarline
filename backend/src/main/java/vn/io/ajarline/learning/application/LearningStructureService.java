package vn.io.ajarline.learning.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.learning.contract.LearningEligibility;
import vn.io.ajarline.learning.contract.LearningStructureQuery;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleArticleEntity;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleArticleJpaRepository;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleEntity;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleJpaRepository;
import vn.io.ajarline.learning.infrastructure.persistence.SeriesEntity;
import vn.io.ajarline.learning.infrastructure.persistence.SeriesJpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Answers the questions that the publication workflow asks about Series and Modules.
 * The caller owns the transaction. Every method fails without one.
 */
@Service
public class LearningStructureService implements LearningEligibility, LearningStructureQuery {
    private final LearningContentPolicy policy;
    private final SeriesJpaRepository seriesRepository;
    private final ModuleJpaRepository moduleRepository;
    private final ModuleArticleJpaRepository moduleArticleRepository;

    public LearningStructureService(LearningContentPolicy policy,
                                    SeriesJpaRepository seriesRepository,
                                    ModuleJpaRepository moduleRepository,
                                    ModuleArticleJpaRepository moduleArticleRepository) {
        this.policy = policy;
        this.seriesRepository = seriesRepository;
        this.moduleRepository = moduleRepository;
        this.moduleArticleRepository = moduleArticleRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void requirePublishableSeries(UUID id) {
        SeriesEntity series = seriesRepository.findById(id).orElseThrow(
                () -> new IllegalStateException("Series subtype row is missing for " + id));
        policy.requirePublishableSeriesOverview(series.getOverviewMarkdown());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void requirePublishableModule(UUID id) {
        ModuleEntity module = moduleRepository.findById(id).orElseThrow(
                () -> new IllegalStateException("Module subtype row is missing for " + id));
        policy.requirePublishableModuleOverview(module.getOverviewMarkdown());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<UUID> moduleIdsOf(UUID seriesId) {
        return moduleRepository.findBySeriesIdOrderByPositionAsc(seriesId).stream()
                .map(ModuleEntity::getContentId)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<UUID> articleIdsOf(UUID moduleId) {
        return moduleArticleRepository.findByModuleIdOrderByPositionAsc(moduleId).stream()
                .map(ModuleArticleEntity::getArticleId)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Map<UUID, List<UUID>> articleIdsOfModules(Collection<UUID> moduleIds) {
        if (moduleIds.isEmpty()) {
            return Map.of();
        }
        return moduleArticleRepository.findByModuleIdIn(moduleIds).stream()
                .collect(Collectors.groupingBy(
                        ModuleArticleEntity::getModuleId,
                        Collectors.mapping(ModuleArticleEntity::getArticleId, Collectors.toList())));
    }
}
