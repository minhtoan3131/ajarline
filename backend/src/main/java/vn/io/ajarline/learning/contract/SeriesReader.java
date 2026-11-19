package vn.io.ajarline.learning.contract;

import java.util.UUID;

public interface SeriesReader {

    /**
     * Returns a Series with its Modules in order, whatever their status.
     *
     * @throws vn.io.ajarline.content.contract.ContentOperationException with
     *         {@code CONTENT_NOT_FOUND} when the id is not a Series
     */
    SeriesView get(UUID id);
}
