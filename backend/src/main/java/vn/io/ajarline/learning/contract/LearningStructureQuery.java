package vn.io.ajarline.learning.contract;

import java.util.List;
import java.util.UUID;

/**
 * Reads who the children of a Series or a Module are.
 * It returns ids only. Every method runs in the caller's transaction and takes no lock.
 */
public interface LearningStructureQuery {

    /**
     * Returns the Module ids of a Series, in position order, whatever their status.
     */
    List<UUID> moduleIdsOf(UUID seriesId);

    /**
     * Returns the Article ids of a Module, in position order, whatever their status.
     */
    List<UUID> articleIdsOf(UUID moduleId);
}
