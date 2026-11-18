package vn.io.ajarline.content.contract;

/**
 * Reads the content index: every content item of any status, newest change first.
 */
public interface ContentIndexReader {

    /**
     * Returns one page of the items that match the filter, ordered by update time
     * (newest first) and then by id. A page after the last one is empty.
     *
     * @param page zero-based page number, at least 0
     * @param size number of items per page, at least 1
     */
    ContentIndexPage find(ContentIndexFilter filter, int page, int size);
}
