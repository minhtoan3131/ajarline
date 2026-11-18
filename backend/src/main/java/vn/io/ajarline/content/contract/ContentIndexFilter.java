package vn.io.ajarline.content.contract;

import java.util.Set;

/**
 * The checked filter of the content index.
 *
 * <p>The two sets are already resolved. An empty set selects nothing. {@code q} is trimmed.
 * It is {@code null} when there is no text filter.</p>
 */
public record ContentIndexFilter(
        Set<ContentType> types,
        Set<PublicationStatus> statuses,
        String q
) {

    public ContentIndexFilter {
        types = Set.copyOf(types);
        statuses = Set.copyOf(statuses);
    }
}
