package vn.io.ajarline.learning.contract;

import java.util.List;
import java.util.UUID;

public interface ModuleOrderEditor {

    /**
     * Replaces the order of all Modules of a Series when {@code expectedRevision} is the
     * revision of the Series. The ids must be exactly the current Modules, each once.
     * An order that does not change writes nothing.
     *
     * @throws LearningOperationException with {@code VALIDATION_FAILED} when an id repeats, or
     *         {@code ORDER_SET_CONFLICT} when the ids are not exactly the current Modules
     * @throws vn.io.ajarline.content.contract.ContentOperationException with
     *         {@code CONTENT_NOT_FOUND} when the id is not a Series, or
     *         {@code REVISION_CONFLICT} when the revision is not current
     */
    ChildOrderView reorderModules(UUID seriesId, long expectedRevision, List<UUID> moduleIds);
}
