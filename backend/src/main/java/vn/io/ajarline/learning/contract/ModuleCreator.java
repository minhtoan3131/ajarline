package vn.io.ajarline.learning.contract;

public interface ModuleCreator {

    /**
     * Creates a draft Module at the given position of its Series. Later Modules move down.
     * The revisions of the Series and of every moved Module rise by one.
     *
     * @throws LearningOperationException with {@code SERIES_REFERENCE_NOT_FOUND}
     *         or {@code INVALID_POSITION}
     */
    ModuleView create(ModuleContentInput input);
}
