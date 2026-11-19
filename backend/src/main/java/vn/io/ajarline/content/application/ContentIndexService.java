package vn.io.ajarline.content.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentIndexFilter;
import vn.io.ajarline.content.contract.ContentIndexPage;
import vn.io.ajarline.content.contract.ContentIndexReader;
import vn.io.ajarline.content.contract.ContentRecordSummary;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemEntity;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemJpaRepository;

import java.util.List;
import java.util.Set;

/**
 * Reads the content index from the common record.
 */
@Service
public class ContentIndexService implements ContentIndexReader {
    private final ContentItemJpaRepository contentRepository;

    public ContentIndexService(ContentItemJpaRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ContentIndexPage find(ContentIndexFilter filter, int page, int size) {
        // An empty set selects nothing, and "IN ()" is not valid SQL.
        if (filter.types().isEmpty() || filter.statuses().isEmpty()) {
            return ContentIndexPage.empty();
        }

        // One more row than the page. A row past the page means there is a next page.
        List<ContentItemEntity> rows = contentRepository.findIndexWindow(
                names(filter.types()),
                names(filter.statuses()),
                filter.q() == null ? "" : filter.q(),
                size + 1,
                (long) page * size);
        boolean hasMore = rows.size() > size;
        List<ContentRecordSummary> items = rows.stream()
                .limit(size)
                .map(ContentRecordService::toSummary)
                .toList();
        return new ContentIndexPage(items, hasMore);
    }

    private static List<String> names(Set<? extends Enum<?>> values) {
        return values.stream().map(Enum::name).toList();
    }
}
