package vn.io.ajarline.learning.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentRecordInput;
import vn.io.ajarline.content.contract.ContentRecordReader;
import vn.io.ajarline.content.contract.ContentRecordSummary;
import vn.io.ajarline.content.contract.ContentRecordView;
import vn.io.ajarline.content.contract.ContentRecordWriter;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.DeletableRecord;
import vn.io.ajarline.content.contract.EditableRecord;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.learning.contract.LearningChild;
import vn.io.ajarline.learning.contract.LearningFailureCode;
import vn.io.ajarline.learning.contract.LearningOperationException;
import vn.io.ajarline.learning.contract.SeriesContentInput;
import vn.io.ajarline.learning.contract.SeriesCreator;
import vn.io.ajarline.learning.contract.SeriesEditor;
import vn.io.ajarline.learning.contract.SeriesReader;
import vn.io.ajarline.learning.contract.SeriesRemover;
import vn.io.ajarline.learning.contract.SeriesView;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleEntity;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleJpaRepository;
import vn.io.ajarline.learning.infrastructure.persistence.SeriesEntity;
import vn.io.ajarline.learning.infrastructure.persistence.SeriesJpaRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Creates, reads and edits Series.
 *
 * <p>The common record belongs to content and is written through its port. The overview belongs
 * to learning. Both are written in one transaction.</p>
 */
@Service
public class SeriesService implements SeriesCreator, SeriesReader, SeriesEditor, SeriesRemover {
    private static final String MODULE_SERIES_CONSTRAINT = "fk_module_series_id";

    private final ContentRecordReader recordReader;
    private final ContentRecordWriter recordWriter;
    private final LearningContentPolicy policy;
    private final SeriesJpaRepository seriesRepository;
    private final ModuleJpaRepository moduleRepository;

    public SeriesService(ContentRecordReader recordReader,
                         ContentRecordWriter recordWriter,
                         LearningContentPolicy policy,
                         SeriesJpaRepository seriesRepository,
                         ModuleJpaRepository moduleRepository) {
        this.recordReader = recordReader;
        this.recordWriter = recordWriter;
        this.policy = policy;
        this.seriesRepository = seriesRepository;
        this.moduleRepository = moduleRepository;
    }

    @Override
    @Transactional
    public SeriesView create(SeriesContentInput input) {
        ContentRecordInput record = recordWriter.normalize(input.record());
        ContentRecordView created = recordWriter.create(ContentType.SERIES, record);
        SeriesEntity series = new SeriesEntity(created.id(), input.overviewMarkdown());

        seriesRepository.save(series);
        return toView(created, series);
    }

    @Override
    @Transactional(readOnly = true)
    public SeriesView get(UUID id) {
        // Read the common record first. A change in between can only cause a later
        // revision conflict, never a wrong reorder.
        ContentRecordView record = recordReader.get(id, ContentType.SERIES);
        return toView(record, requireSeries(id));
    }

    @Override
    @Transactional
    public SeriesView replace(UUID id, long expectedRevision, SeriesContentInput input) {
        ContentRecordInput record = recordWriter.normalize(input.record());
        // Lock the row before any other read. Then no stale copy of it is in the
        // persistence context when the revision is compared.
        EditableRecord editable = recordWriter.lockForEdit(
                id, ContentType.SERIES, expectedRevision, record);
        SeriesEntity series = requireSeries(id);

        if (editable.current().status() == PublicationStatus.PUBLISHED) {
            policy.requirePublishableSeriesOverview(input.overviewMarkdown());
        }

        boolean hasOverviewChanged = !series.hasSameOverview(input.overviewMarkdown());
        ContentRecordView saved = recordWriter.replace(editable, record, hasOverviewChanged);
        if (hasOverviewChanged) {
            series.replaceOverview(input.overviewMarkdown());
        }
        return toView(saved, series);
    }

    @Override
    @Transactional
    public void delete(UUID id, long expectedRevision) {
        // Everyone who adds a Module locks the Series first, so none can appear after the check.
        DeletableRecord record = recordWriter.lockForDelete(id, ContentType.SERIES, expectedRevision);
        if (moduleRepository.existsBySeriesId(id)) {
            throw stillInUse();
        }
        try {
            recordWriter.deleteRecord(record);
        } catch (DataIntegrityViolationException exception) {
            // The foreign key is the last guard. Other violations are not hidden.
            if (containsConstraint(exception, MODULE_SERIES_CONSTRAINT)) {
                throw stillInUse();
            }
            throw exception;
        }
    }

    private LearningOperationException stillInUse() {
        return new LearningOperationException(
                LearningFailureCode.CONTENT_IN_USE, "The Series still has Modules.");
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

    private SeriesEntity requireSeries(UUID id) {
        return seriesRepository.findById(id).orElseThrow(
                () -> new IllegalStateException("Series subtype row is missing for " + id));
    }

    private SeriesView toView(ContentRecordView record, SeriesEntity series) {
        List<ModuleEntity> modules = moduleRepository.findBySeriesIdOrderByPositionAsc(record.id());
        Map<UUID, ContentRecordSummary> summaries = recordReader.summariesOf(
                modules.stream().map(ModuleEntity::getContentId).toList());
        List<LearningChild> children = modules.stream()
                .map(module -> toChild(summaries.get(module.getContentId()), module.getPosition()))
                .toList();
        return new SeriesView(
                record,
                recordReader.topicIdsOf(record.id()),
                series.getOverviewMarkdown(),
                children);
    }

    private LearningChild toChild(ContentRecordSummary summary, int position) {
        return new LearningChild(
                summary.id(), summary.title(), summary.slug(), summary.status(), position);
    }
}
