package vn.io.ajarline.learning.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentRecordWriter;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PlacementChange;
import vn.io.ajarline.learning.contract.LearningFailureCode;
import vn.io.ajarline.learning.contract.LearningOperationException;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleEntity;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleJpaRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Puts Modules in their Series and keeps positions continuous.
 *
 * <p>Every change to the Modules of a Series must lock the Series first. The lock makes changes
 * wait for each other. The database cannot detect a gap in positions, so this lock is the only
 * guard against one.</p>
 *
 * <p>The methods write positions but raise no revision. They return the ids that need it. The
 * caller raises them once, after its own writes.</p>
 */
@Component
public class HierarchyPlacement {
    private final ContentRecordWriter recordWriter;
    private final ModuleJpaRepository moduleRepository;
    private final PositionPolicy positions;

    public HierarchyPlacement(ContentRecordWriter recordWriter,
                              ModuleJpaRepository moduleRepository,
                              PositionPolicy positions) {
        this.recordWriter = recordWriter;
        this.moduleRepository = moduleRepository;
        this.positions = positions;
    }

    /**
     * Locks the Series a Module leaves and the Series it goes to, in ascending id order.
     * This is the only place that locks Series.
     *
     * @param currentSeriesId null when the Module does not exist yet
     * @throws LearningOperationException with {@code SERIES_REFERENCE_NOT_FOUND}
     *         when the target is not a Series
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockStructure(UUID currentSeriesId, UUID targetSeriesId) {
        Set<UUID> seriesIds = new TreeSet<>();
        seriesIds.add(targetSeriesId);
        if (currentSeriesId != null) {
            seriesIds.add(currentSeriesId);
        }
        for (UUID seriesId : seriesIds) {
            boolean exists = recordWriter.lockStructure(seriesId, ContentType.SERIES);
            if (!exists && seriesId.equals(targetSeriesId)) {
                throw new LearningOperationException(
                        LearningFailureCode.SERIES_REFERENCE_NOT_FOUND,
                        "Series was not found: " + seriesId);
            }
        }
    }

    /**
     * Saves a new Module at its position and moves later Modules down. The Series must be locked.
     * {@code INVALID_POSITION} is thrown before anything is written.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public PlacementChange placeNew(ModuleEntity module) {
        UUID seriesId = module.getSeriesId();
        List<ModuleEntity> siblings = siblingsOf(seriesId);

        List<PositionAssignment> opening = positions.insert(
                idsOf(siblings), module.getContentId(), module.getPosition());
        apply(siblings, opening);
        moduleRepository.save(module);

        return PlacementChange.of(revisionTargets(
                module.getContentId(), List.of(seriesId), opening));
    }

    /**
     * Puts an existing Module at a position of a Series. It may be the same Series.
     * Both Series must be locked. Returns {@link PlacementChange#UNCHANGED} when the Module is already there.
     * {@code INVALID_POSITION} is thrown before anything is written.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public PlacementChange place(ModuleEntity module, UUID targetSeriesId, int position) {
        UUID currentSeriesId = module.getSeriesId();
        UUID moduleId = module.getContentId();

        if (currentSeriesId.equals(targetSeriesId)) {
            List<ModuleEntity> siblings = siblingsOf(currentSeriesId);
            List<PositionAssignment> moving = positions.move(idsOf(siblings), moduleId, position);
            if (moving.isEmpty()) {
                return PlacementChange.UNCHANGED;
            }
            apply(siblings, moving);
            return PlacementChange.of(revisionTargets(moduleId, List.of(currentSeriesId), moving));
        }

        List<ModuleEntity> oldSiblings = siblingsOf(currentSeriesId);
        List<ModuleEntity> newSiblings = siblingsOf(targetSeriesId);
        // Compute both results before any write. An invalid position then changes nothing.
        List<PositionAssignment> closing = positions.detach(idsOf(oldSiblings), moduleId);
        List<PositionAssignment> opening = positions.insert(idsOf(newSiblings), moduleId, position);
        apply(oldSiblings, closing);
        apply(newSiblings, opening);
        module.moveTo(targetSeriesId, position);

        return PlacementChange.of(revisionTargets(
                moduleId, List.of(currentSeriesId, targetSeriesId), closing, opening));
    }

    private List<ModuleEntity> siblingsOf(UUID seriesId) {
        return moduleRepository.findBySeriesIdOrderByPositionAsc(seriesId);
    }

    private List<UUID> idsOf(List<ModuleEntity> siblings) {
        return siblings.stream().map(ModuleEntity::getContentId).toList();
    }

    /**
     * Writes new positions through the entities. An id not in the list is the placed Module.
     * The caller moves that one.
     */
    private void apply(List<ModuleEntity> siblings, List<PositionAssignment> assignments) {
        Map<UUID, ModuleEntity> byId = new HashMap<>();
        for (ModuleEntity sibling : siblings) {
            byId.put(sibling.getContentId(), sibling);
        }
        for (PositionAssignment assignment : assignments) {
            ModuleEntity sibling = byId.get(assignment.id());
            if (sibling != null) {
                sibling.changePosition(assignment.position());
            }
        }
    }

    /**
     * Collects the parents and siblings whose revision must rise. The placed Module is left out.
     */
    @SafeVarargs
    private Set<UUID> revisionTargets(
            UUID placedId, Collection<UUID> parentIds, List<PositionAssignment>... assignmentLists) {
        Set<UUID> targets = new LinkedHashSet<>();
        for (List<PositionAssignment> assignments : assignmentLists) {
            for (PositionAssignment assignment : assignments) {
                if (!assignment.id().equals(placedId)) {
                    targets.add(assignment.id());
                }
            }
        }
        targets.addAll(parentIds);
        return targets;
    }
}
