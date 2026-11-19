package vn.io.ajarline.learning.contract;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Tells what readers can see in the learning hierarchy.
 * Every method runs in the caller's transaction and takes no lock.
 */
public interface LearningVisibility {

    /**
     * Returns the ids, among the given Modules, of the Modules that readers can see now.
     * A Module is public when it is published and has a published Article.
     */
    Set<UUID> publicModuleIds(Collection<UUID> moduleIds);
}
