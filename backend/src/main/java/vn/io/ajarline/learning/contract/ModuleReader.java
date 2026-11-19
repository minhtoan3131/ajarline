package vn.io.ajarline.learning.contract;

import java.util.UUID;

public interface ModuleReader {

    /**
     * Returns a Module with its Articles in order, whatever their status.
     *
     * @throws vn.io.ajarline.content.contract.ContentOperationException with
     *         {@code CONTENT_NOT_FOUND} when the id is not a Module
     */
    ModuleView get(UUID id);
}
