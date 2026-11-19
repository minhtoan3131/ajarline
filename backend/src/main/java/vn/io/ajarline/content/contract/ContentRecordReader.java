package vn.io.ajarline.content.contract;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the common record of content items of any type.
 * Every method runs in the caller's transaction. The reads take no lock.
 */
public interface ContentRecordReader {

    /**
     * @throws ContentOperationException with {@link ContentFailureCode#CONTENT_NOT_FOUND}
     *         when the id is unknown or the item has another type
     */
    ContentRecordView get(UUID id, ContentType type);

    /**
     * Returns the Topic ids of the item, ordered by id.
     */
    List<UUID> topicIdsOf(UUID id);

    /**
     * Reads many items with one query. An unknown id has no key.
     */
    Map<UUID, ContentRecordSummary> summariesOf(Collection<UUID> ids);

    /**
     * Finds the id of the item with this type and slug. It gives nothing when no item has both,
     * so a slug of another type is the same as an unknown slug.
     */
    Optional<UUID> idOf(ContentType type, String slug);
}
