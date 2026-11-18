package vn.io.ajarline.content.contract;

import java.util.Set;
import java.util.UUID;

/**
 * Result of placing a child. It lists the items whose revision must rise.
 *
 * <p>The ids are the siblings that moved and the parents whose children changed. The placed
 * child is not in the set. The caller raises the revisions once, after all its own writes.</p>
 */
public record PlacementChange(boolean changed, Set<UUID> revisionTargets) {

    /** The child was already where it was asked to be. */
    public static final PlacementChange UNCHANGED = new PlacementChange(false, Set.of());

    public PlacementChange {
        revisionTargets = Set.copyOf(revisionTargets);
    }

    public static PlacementChange of(Set<UUID> revisionTargets) {
        return new PlacementChange(true, revisionTargets);
    }
}
