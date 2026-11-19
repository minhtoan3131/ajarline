package vn.io.ajarline.content.contract;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tells what readers can see in the learning hierarchy. Learning implements it.
 * Every method must run in the caller's transaction and takes no lock.
 */
public interface PublicLearningContextPort {

    /**
     * Returns the ids, among the given Module and Series ids, of those that readers can see now.
     * A Module is public when it is published and has a public Article. A Series is public when
     * it is published and has a public Module. An id that matches no item is ignored.
     */
    Set<UUID> publicAmong(Collection<UUID> moduleOrSeriesIds);

    /**
     * Returns the public parents of many Articles and Modules with one read.
     * Every given id has a key. An id with no public parent has an empty context.
     */
    Map<UUID, PublicContentContext> contextsOf(Collection<UUID> articleOrModuleIds);
}
