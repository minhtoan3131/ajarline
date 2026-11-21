package vn.io.ajarline.learning.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.content.contract.ContentRecordInput;
import vn.io.ajarline.content.contract.ContentRecordReader;
import vn.io.ajarline.content.contract.ContentRecordSummary;
import vn.io.ajarline.content.contract.ContentRecordView;
import vn.io.ajarline.content.contract.ContentRecordWriter;
import vn.io.ajarline.content.contract.ContentRevisionCommands;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.DeletableRecord;
import vn.io.ajarline.content.contract.EditableRecord;
import vn.io.ajarline.content.contract.PlacementChange;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.learning.contract.LearningChild;
import vn.io.ajarline.learning.contract.LearningFailureCode;
import vn.io.ajarline.learning.contract.LearningOperationException;
import vn.io.ajarline.learning.contract.ModuleContentInput;
import vn.io.ajarline.learning.contract.ModuleCreator;
import vn.io.ajarline.learning.contract.ModuleEditor;
import vn.io.ajarline.learning.contract.ModuleReader;
import vn.io.ajarline.learning.contract.ModuleRemover;
import vn.io.ajarline.learning.contract.ModuleView;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleArticleEntity;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleArticleJpaRepository;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleEntity;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleJpaRepository;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Creates, reads and edits Modules, including their place in the Series.
 *
 * <p>Placing a Module also changes its siblings and its Series. So the use case locks the
 * Series first, writes everything, and raises the other revisions last.</p>
 */
@Service
public class ModuleService implements ModuleCreator, ModuleReader, ModuleEditor, ModuleRemover {
    private static final String MEMBER_MODULE_CONSTRAINT = "fk_module_article_module_id";

    private final ContentRecordReader recordReader;
    private final ContentRecordWriter recordWriter;
    private final ContentRevisionCommands revisions;
    private final LearningContentPolicy policy;
    private final HierarchyPlacement placement;
    private final ModuleJpaRepository moduleRepository;
    private final ModuleArticleJpaRepository moduleArticleRepository;
    private final Clock clock;

    public ModuleService(ContentRecordReader recordReader,
                         ContentRecordWriter recordWriter,
                         ContentRevisionCommands revisions,
                         LearningContentPolicy policy,
                         HierarchyPlacement placement,
                         ModuleJpaRepository moduleRepository,
                         ModuleArticleJpaRepository moduleArticleRepository,
                         Clock clock) {
        this.recordReader = recordReader;
        this.recordWriter = recordWriter;
        this.revisions = revisions;
        this.policy = policy;
        this.placement = placement;
        this.moduleRepository = moduleRepository;
        this.moduleArticleRepository = moduleArticleRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ModuleView create(ModuleContentInput input) {
        ContentRecordInput record = recordWriter.normalize(input.record());
        placement.lockStructure(null, input.seriesId());
        ContentRecordView created = recordWriter.create(ContentType.MODULE, record);
        ModuleEntity module = new ModuleEntity(
                created.id(), input.seriesId(), input.position(), input.overviewMarkdown());

        PlacementChange change = placement.placeNew(module);
        // Last, because this statement clears the persistence context.
        revisions.advanceRevisions(change.revisionTargets(), clock.instant());
        return toView(created, module);
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleView get(UUID id) {
        ContentRecordView record = recordReader.get(id, ContentType.MODULE);
        return toView(record, requireModuleRow(id));
    }

    @Override
    @Transactional
    public ModuleView replace(UUID id, long expectedRevision, ModuleContentInput input) {
        ContentRecordInput record = recordWriter.normalize(input.record());

        // Read the Module row, not the common record. A copy of the common record in the
        // persistence context would hide the fresh row from the locking read below.
        ModuleEntity module = moduleRepository.findById(id)
                .orElseThrow(() -> new ContentOperationException(
                        ContentFailureCode.CONTENT_NOT_FOUND, "MODULE was not found: " + id));
        placement.lockStructure(module.getSeriesId(), input.seriesId());
        EditableRecord editable = recordWriter.lockForEdit(
                id, ContentType.MODULE, expectedRevision, record);

        if (editable.current().status() == PublicationStatus.PUBLISHED) {
            policy.requirePublishableModuleOverview(input.overviewMarkdown());
        }

        // Write the placement before the "nothing changed" decision. A later failure
        // then rolls back the move with the rest of the save.
        PlacementChange change = placement.place(module, input.seriesId(), input.position());
        boolean hasOverviewChanged = !module.hasSameOverview(input.overviewMarkdown());
        ContentRecordView saved = recordWriter.replace(
                editable, record, hasOverviewChanged || change.changed());
        if (hasOverviewChanged) {
            module.replaceOverview(input.overviewMarkdown());
        }

        // Last, because this statement clears the persistence context. The Module
        // is not in the set: `replace` already raised its revision.
        revisions.advanceRevisions(change.revisionTargets(), clock.instant());
        return toView(saved, module);
    }

    @Override
    @Transactional
    public void delete(UUID id, long expectedRevision) {
        // Read the Module row, not the common record. A copy of the common record in the
        // persistence context would hide the fresh row from the locking read below.
        ModuleEntity module = moduleRepository.findById(id)
                .orElseThrow(() -> new ContentOperationException(
                        ContentFailureCode.CONTENT_NOT_FOUND, "MODULE was not found: " + id));
        // Parent before child: lock the Series, then the Module.
        placement.lockStructure(module.getSeriesId(), null);
        DeletableRecord record = recordWriter.lockForDelete(id, ContentType.MODULE, expectedRevision);

        // Check before any write. Everyone who adds an Article locks the Module first.
        if (moduleArticleRepository.existsByModuleId(id)) {
            throw stillInUse();
        }
        PlacementChange change = placement.detachModule(id);
        try {
            recordWriter.deleteRecord(record);
        } catch (DataIntegrityViolationException exception) {
            // The foreign key is the last guard. Other violations are not hidden.
            if (containsConstraint(exception, MEMBER_MODULE_CONSTRAINT)) {
                throw stillInUse();
            }
            throw exception;
        }
        // Last, because this statement clears the persistence context. The deleted Module is
        // not in the set.
        revisions.advanceRevisions(change.revisionTargets(), clock.instant());
    }

    private LearningOperationException stillInUse() {
        return new LearningOperationException(
                LearningFailureCode.CONTENT_IN_USE, "The Module still has Articles.");
    }

    private boolean containsConstraint(Throwable failure, String constraint) {
        Throwable current = failure;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(constraint)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ModuleEntity requireModuleRow(UUID id) {
        return moduleRepository.findById(id).orElseThrow(
                () -> new IllegalStateException("Module subtype row is missing for " + id));
    }

    private ModuleView toView(ContentRecordView record, ModuleEntity module) {
        List<ModuleArticleEntity> memberships =
                moduleArticleRepository.findByModuleIdOrderByPositionAsc(record.id());
        Map<UUID, ContentRecordSummary> summaries = recordReader.summariesOf(
                memberships.stream().map(ModuleArticleEntity::getArticleId).toList());
        List<LearningChild> articles = memberships.stream()
                .map(membership -> toChild(
                        summaries.get(membership.getArticleId()), membership.getPosition()))
                .toList();
        return new ModuleView(
                record,
                recordReader.topicIdsOf(record.id()),
                module.getOverviewMarkdown(),
                module.getSeriesId(),
                module.getPosition(),
                articles);
    }

    private LearningChild toChild(ContentRecordSummary summary, int position) {
        return new LearningChild(
                summary.id(), summary.title(), summary.slug(), summary.status(), position);
    }
}
