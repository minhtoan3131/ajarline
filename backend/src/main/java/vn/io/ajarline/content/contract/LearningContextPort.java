package vn.io.ajarline.content.contract;

import java.util.Optional;
import java.util.UUID;

/**
 * Reads what learning knows about an Article. Learning implements it.
 * The read runs in the caller's transaction and takes no lock.
 */
public interface LearningContextPort {

    /**
     * Returns the Module and position of the Article, or empty when it is standalone.
     */
    Optional<ArticlePlacement> placementOf(UUID articleId);
}
