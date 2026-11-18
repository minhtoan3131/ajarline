package vn.io.ajarline.learning.contract;

import java.util.UUID;

public interface ModuleEditor {

    /**
     * Replaces the editable state when {@code expectedRevision} is current.
     * If the place changes, the revisions of both Series and of the moved Modules rise by one.
     * A published Module must keep its summary and overview.
     *
     * @throws LearningOperationException with {@code SERIES_REFERENCE_NOT_FOUND},
     *         {@code INVALID_POSITION} or {@code MODULE_OVERVIEW_REQUIRED}
     */
    ModuleView replace(UUID id, long expectedRevision, ModuleContentInput input);
}
