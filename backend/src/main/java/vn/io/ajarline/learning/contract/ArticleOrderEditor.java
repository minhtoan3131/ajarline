package vn.io.ajarline.learning.contract;

import java.util.List;
import java.util.UUID;

public interface ArticleOrderEditor {

    /**
     * Replaces the order of all Articles of a Module when {@code expectedRevision} is the
     * revision of the Module. The ids must be exactly the current Articles, each once.
     * An order that does not change writes nothing.
     *
     * @throws LearningOperationException with {@code VALIDATION_FAILED} when an id repeats, or
     *         {@code ORDER_SET_CONFLICT} when the ids are not exactly the current Articles
     * @throws vn.io.ajarline.content.contract.ContentOperationException with
     *         {@code CONTENT_NOT_FOUND} when the id is not a Module, or
     *         {@code REVISION_CONFLICT} when the revision is not current
     */
    ChildOrderView reorderArticles(UUID moduleId, long expectedRevision, List<UUID> articleIds);
}
