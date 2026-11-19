package vn.io.ajarline.content.contract;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface ContentRevisionCommands {

    /**
     * Advances the revision of every listed content item by one and sets
     * their update time. Ids that match no item are ignored. The call joins
     * the transaction of the caller and fails when there is none.
     */
    void advanceRevisions(Collection<UUID> contentIds, Instant changedAt);
}
