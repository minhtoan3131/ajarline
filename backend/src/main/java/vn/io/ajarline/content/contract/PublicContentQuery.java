package vn.io.ajarline.content.contract;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface PublicContentQuery {

    /**
     * Returns the ids, among the given ones, of the items that are public now.
     * Ids that match no item are ignored.
     */
    Set<UUID> findPublicIds(Collection<UUID> contentIds);

    /**
     * Returns one page of the public items among the given ids.
     *
     * <p>Only items whose type is in {@code types} are kept. The order is by
     * type: series, module, article, news. Series, modules and articles are
     * then ordered by title and id; news by publication time, newest first,
     * and id. Paging is applied after filtering and ordering. A page after
     * the last one is empty.</p>
     *
     * @param page zero-based page number, at least 0
     * @param size number of items per page, at least 1
     */
    PublicContentPage findPublicPage(
            Collection<UUID> contentIds,
            Set<ContentType> types,
            int page,
            int size);
}
