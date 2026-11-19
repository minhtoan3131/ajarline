package vn.io.ajarline.learning.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentRecordWriter;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PlacementChange;
import vn.io.ajarline.learning.contract.LearningFailureCode;
import vn.io.ajarline.learning.contract.LearningOperationException;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleArticleEntity;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleArticleJpaRepository;
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
import java.util.function.Function;
import java.util.function.ObjIntConsumer;

/**
 * Puts Modules in their Series and Articles in their Module, and keeps positions continuous.
 *
 * <p>Every change to the children of a parent must lock the parent first. The lock makes changes
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
    private final ModuleArticleJpaRepository memberRepository;
    private final PositionPolicy positions;

    public HierarchyPlacement(ContentRecordWriter recordWriter,
                              ModuleJpaRepository moduleRepository,
                              ModuleArticleJpaRepository memberRepository,
                              PositionPolicy positions) {
        this.recordWriter = recordWriter;
        this.moduleRepository = moduleRepository;
        this.memberRepository = memberRepository;
        this.positions = positions;
    }

    /**
     * Locks the Series a Module leaves and the Series it goes to, in ascending id order.
     *
     * @param currentSeriesId null when the Module does not exist yet
     * @throws LearningOperationException with {@code SERIES_REFERENCE_NOT_FOUND}
     *         when the target is not a Series
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockStructure(UUID currentSeriesId, UUID targetSeriesId) {
        lockParents(currentSeriesId, targetSeriesId, ContentType.SERIES,
                seriesId -> new LearningOperationException(
                        LearningFailureCode.SERIES_REFERENCE_NOT_FOUND,
                        "Series was not found: " + seriesId));
    }

    /**
     * Locks the Module an Article leaves and the Module it goes to, in ascending id order.
     * The Article does not need to exist yet.
     *
     * @param targetModuleId null when the Article becomes standalone
     * @throws LearningOperationException with {@code MODULE_REFERENCE_NOT_FOUND}
     *         when the target is not a Module
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockArticleStructure(UUID articleId, UUID targetModuleId) {
        // A scalar query, not an entity. No copy of the row stays in the persistence context.
        UUID currentModuleId = memberRepository.findModuleIdByArticleId(articleId).orElse(null);
        lockParents(currentModuleId, targetModuleId, ContentType.MODULE,
                moduleId -> new LearningOperationException(
                        LearningFailureCode.MODULE_REFERENCE_NOT_FOUND,
                        "Module was not found: " + moduleId));
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
                moduleIdsOf(siblings), module.getContentId(), module.getPosition());
        applyToModules(siblings, opening);
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
        UUID moduleId = module.getContentId();
        UUID currentSeriesId = module.getSeriesId();
        boolean sameSeries = currentSeriesId.equals(targetSeriesId);

        List<ModuleEntity> oldSiblings = siblingsOf(currentSeriesId);
        List<ModuleEntity> newSiblings = sameSeries ? oldSiblings : siblingsOf(targetSeriesId);
        Relocation relocation = plan(
                moduleIdsOf(oldSiblings), moduleIdsOf(newSiblings), moduleId, sameSeries, position);
        if (relocation.isEmpty()) {
            return PlacementChange.UNCHANGED;
        }

        applyToModules(oldSiblings, relocation.closing());
        applyToModules(newSiblings, relocation.opening());
        if (!sameSeries) {
            module.moveTo(targetSeriesId, position);
        }
        return PlacementChange.of(revisionTargets(
                moduleId, List.of(currentSeriesId, targetSeriesId),
                relocation.closing(), relocation.opening()));
    }

    /**
     * Puts an Article at a position of a Module. The Article may be in no Module, in this Module,
     * or in another one. Both Modules must be locked.
     * Returns {@link PlacementChange#UNCHANGED} when the Article is already there.
     * {@code INVALID_POSITION} is thrown before anything is written.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public PlacementChange placeArticle(UUID articleId, UUID targetModuleId, int position) {
        // Load the row only now, after the Modules are locked. Its position is then current.
        ModuleArticleEntity membership = memberRepository.findById(articleId).orElse(null);
        UUID currentModuleId = membership == null ? null : membership.getModuleId();
        boolean sameModule = targetModuleId.equals(currentModuleId);

        List<ModuleArticleEntity> oldMembers =
                currentModuleId == null ? List.of() : membersOf(currentModuleId);
        List<ModuleArticleEntity> newMembers = sameModule ? oldMembers : membersOf(targetModuleId);
        Relocation relocation = plan(
                currentModuleId == null ? null : articleIdsOf(oldMembers),
                articleIdsOf(newMembers), articleId, sameModule, position);
        if (relocation.isEmpty()) {
            return PlacementChange.UNCHANGED;
        }

        applyToMembers(oldMembers, relocation.closing());
        applyToMembers(newMembers, relocation.opening());
        if (membership == null) {
            memberRepository.save(new ModuleArticleEntity(articleId, targetModuleId, position));
        } else if (!sameModule) {
            membership.moveTo(targetModuleId, position);
        }

        Set<UUID> parentIds = new LinkedHashSet<>();
        if (currentModuleId != null) {
            parentIds.add(currentModuleId);
        }
        parentIds.add(targetModuleId);
        return PlacementChange.of(revisionTargets(
                articleId, parentIds, relocation.closing(), relocation.opening()));
    }

    /**
     * Takes an Article out of its Module and moves the later Articles up.
     * The Module must be locked. Returns {@link PlacementChange#UNCHANGED} when the Article is standalone.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public PlacementChange detachArticle(UUID articleId) {
        ModuleArticleEntity membership = memberRepository.findById(articleId).orElse(null);
        if (membership == null) {
            return PlacementChange.UNCHANGED;
        }

        UUID moduleId = membership.getModuleId();
        List<ModuleArticleEntity> members = membersOf(moduleId);
        List<PositionAssignment> closing = positions.detach(articleIdsOf(members), articleId);
        applyToMembers(members, closing);
        memberRepository.delete(membership);

        return PlacementChange.of(revisionTargets(articleId, List.of(moduleId), closing));
    }

    /**
     * This and its callers are the only code that locks parents. It sorts the ids first,
     * so two callers can never wait for each other.
     */
    private void lockParents(UUID currentId, UUID targetId, ContentType type,
                             Function<UUID, LearningOperationException> notFound) {
        Set<UUID> parentIds = new TreeSet<>();
        if (targetId != null) {
            parentIds.add(targetId);
        }
        if (currentId != null) {
            parentIds.add(currentId);
        }
        for (UUID parentId : parentIds) {
            boolean exists = recordWriter.lockStructure(parentId, type);
            if (!exists && parentId.equals(targetId)) {
                throw notFound.apply(parentId);
            }
        }
    }

    /**
     * Works out the new positions of a child that changes parent, changes place inside its
     * parent, or enters a parent for the first time. It reads nothing and writes nothing.
     *
     * @param currentIds the children of the parent it is in now; null when it is in none
     * @param targetIds the children of the target parent
     */
    private Relocation plan(List<UUID> currentIds, List<UUID> targetIds, UUID childId,
                            boolean sameParent, int position) {
        if (sameParent) {
            return new Relocation(List.of(), positions.move(targetIds, childId, position));
        }
        // Compute both results before any write. An invalid position then changes nothing.
        List<PositionAssignment> closing =
                currentIds == null ? List.of() : positions.detach(currentIds, childId);
        List<PositionAssignment> opening = positions.insert(targetIds, childId, position);
        return new Relocation(closing, opening);
    }

    private List<ModuleEntity> siblingsOf(UUID seriesId) {
        return moduleRepository.findBySeriesIdOrderByPositionAsc(seriesId);
    }

    private List<ModuleArticleEntity> membersOf(UUID moduleId) {
        return memberRepository.findByModuleIdOrderByPositionAsc(moduleId);
    }

    private List<UUID> moduleIdsOf(List<ModuleEntity> siblings) {
        return siblings.stream().map(ModuleEntity::getContentId).toList();
    }

    private List<UUID> articleIdsOf(List<ModuleArticleEntity> members) {
        return members.stream().map(ModuleArticleEntity::getArticleId).toList();
    }

    private void applyToModules(List<ModuleEntity> siblings, List<PositionAssignment> assignments) {
        apply(siblings, ModuleEntity::getContentId, ModuleEntity::changePosition, assignments);
    }

    private void applyToMembers(List<ModuleArticleEntity> members, List<PositionAssignment> assignments) {
        apply(members, ModuleArticleEntity::getArticleId, ModuleArticleEntity::changePosition, assignments);
    }

    /**
     * Writes new positions through the entities. An id not in the list is the placed child.
     * The caller moves that one.
     */
    private <T> void apply(List<T> children, Function<T, UUID> idOf,
                           ObjIntConsumer<T> setPosition, List<PositionAssignment> assignments) {
        Map<UUID, T> byId = new HashMap<>();
        for (T child : children) {
            byId.put(idOf.apply(child), child);
        }
        for (PositionAssignment assignment : assignments) {
            T child = byId.get(assignment.id());
            if (child != null) {
                setPosition.accept(child, assignment.position());
            }
        }
    }

    /**
     * Collects the parents and siblings whose revision must rise. The placed child is left out.
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

    /**
     * The position changes of one placement. {@code closing} is for the parent the child leaves,
     * {@code opening} for the parent it enters.
     */
    private record Relocation(List<PositionAssignment> closing, List<PositionAssignment> opening) {

        boolean isEmpty() {
            return closing.isEmpty() && opening.isEmpty();
        }
    }
}
