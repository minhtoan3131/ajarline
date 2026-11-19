package vn.io.ajarline.learning.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentRecordReader;
import vn.io.ajarline.content.contract.ContentRecordSummary;
import vn.io.ajarline.content.contract.ContentRecordWriter;
import vn.io.ajarline.content.contract.ContentRevisionCommands;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.learning.contract.ArticleOrderEditor;
import vn.io.ajarline.learning.contract.ChildOrderView;
import vn.io.ajarline.learning.contract.ChildPosition;
import vn.io.ajarline.learning.contract.ModuleOrderEditor;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleArticleEntity;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleArticleJpaRepository;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleEntity;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleJpaRepository;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

/**
 * Replaces the whole order of the children of a Series or a Module.
 *
 * <p>The use case locks the parent and compares its revision first. Only then does it read the
 * children, so the set it compares is the set that stays until the transaction ends.</p>
 */
@Service
public class ChildOrderService implements ModuleOrderEditor, ArticleOrderEditor {
    private final ContentRecordReader recordReader;
    private final ContentRecordWriter recordWriter;
    private final ContentRevisionCommands revisions;
    private final PositionPolicy positions;
    private final ModuleJpaRepository moduleRepository;
    private final ModuleArticleJpaRepository memberRepository;
    private final Clock clock;

    public ChildOrderService(ContentRecordReader recordReader,
                             ContentRecordWriter recordWriter,
                             ContentRevisionCommands revisions,
                             PositionPolicy positions,
                             ModuleJpaRepository moduleRepository,
                             ModuleArticleJpaRepository memberRepository,
                             Clock clock) {
        this.recordReader = recordReader;
        this.recordWriter = recordWriter;
        this.revisions = revisions;
        this.positions = positions;
        this.moduleRepository = moduleRepository;
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ChildOrderView reorderModules(UUID seriesId, long expectedRevision, List<UUID> moduleIds) {
        return reorder(seriesId, ContentType.SERIES, expectedRevision, moduleIds,
                () -> moduleRepository.findBySeriesIdOrderByPositionAsc(seriesId),
                ModuleEntity::getContentId, ModuleEntity::changePosition);
    }

    @Override
    @Transactional
    public ChildOrderView reorderArticles(UUID moduleId, long expectedRevision, List<UUID> articleIds) {
        return reorder(moduleId, ContentType.MODULE, expectedRevision, articleIds,
                () -> memberRepository.findByModuleIdOrderByPositionAsc(moduleId),
                ModuleArticleEntity::getArticleId, ModuleArticleEntity::changePosition);
    }

    /**
     * The steps are the same for both parents. Only the parent type, the way to read the
     * children and the way to write a position differ, so the caller passes those in.
     */
    private <T> ChildOrderView reorder(
            UUID parentId, ContentType parentType, long expectedRevision, List<UUID> requestedIds,
            Supplier<List<T>> loadChildren, Function<T, UUID> idOf, ObjIntConsumer<T> setPosition) {
        // No database needed, so this runs before any lock.
        positions.requireDistinct(requestedIds);
        recordWriter.lockForEdit(parentId, parentType, expectedRevision);

        // Read the children only after the lock. A parent change cannot slip in any more.
        List<T> children = loadChildren.get();
        List<PositionAssignment> changes = positions.reorder(
                children.stream().map(idOf).toList(), requestedIds);

        if (!changes.isEmpty()) {
            Map<UUID, T> byId = new HashMap<>();
            for (T child : children) {
                byId.put(idOf.apply(child), child);
            }
            Set<UUID> revisionTargets = new LinkedHashSet<>();
            for (PositionAssignment change : changes) {
                setPosition.accept(byId.get(change.id()), change.position());
                revisionTargets.add(change.id());
            }
            revisionTargets.add(parentId);
            // Last write, because this statement clears the persistence context.
            revisions.advanceRevisions(revisionTargets, clock.instant());
        }
        return readOrder(parentId, requestedIds);
    }

    /**
     * Reads the revisions after the change. Before it, the entities hold the old ones.
     */
    private ChildOrderView readOrder(UUID parentId, List<UUID> orderedIds) {
        List<UUID> ids = new ArrayList<>(orderedIds);
        ids.add(parentId);
        Map<UUID, ContentRecordSummary> summaries = recordReader.summariesOf(ids);

        List<ChildPosition> children = new ArrayList<>();
        for (int index = 0; index < orderedIds.size(); index++) {
            UUID id = orderedIds.get(index);
            children.add(new ChildPosition(id, index + 1, summaries.get(id).revision()));
        }
        return new ChildOrderView(parentId, summaries.get(parentId).revision(), children);
    }
}
