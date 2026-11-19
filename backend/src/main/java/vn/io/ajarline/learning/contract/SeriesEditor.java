package vn.io.ajarline.learning.contract;

import java.util.UUID;

public interface SeriesEditor {

    /**
     * Replaces the editable state when {@code expectedRevision} is current.
     * A published Series must keep its summary and its overview.
     *
     * @throws LearningOperationException with {@code SERIES_OVERVIEW_REQUIRED}
     *         when a published Series would lose its overview
     */
    SeriesView replace(UUID id, long expectedRevision, SeriesContentInput input);
}
