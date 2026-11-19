package vn.io.ajarline.learning.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicContentPage;
import vn.io.ajarline.content.contract.PublicContentQuery;
import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.TopicAssignmentPort;
import vn.io.ajarline.content.contract.TopicLabel;
import vn.io.ajarline.learning.infrastructure.persistence.SeriesJpaRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runs the anonymous reads of Series and Modules.
 *
 * <p>Content decides which Series are public, in which order, and cuts the page. This service
 * only knows which items are Series, and adds their Topics.</p>
 */
@Service
public class PublicLearningService {
    private final SeriesJpaRepository seriesRepository;
    private final PublicContentQuery publicContent;
    private final TopicAssignmentPort topicAssignments;

    public PublicLearningService(SeriesJpaRepository seriesRepository,
                                 PublicContentQuery publicContent,
                                 TopicAssignmentPort topicAssignments) {
        this.seriesRepository = seriesRepository;
        this.publicContent = publicContent;
        this.topicAssignments = topicAssignments;
    }

    /**
     * Returns one page of the public Series, ordered by title and then by id, each with its Topics.
     * A page after the last one is empty. The Topics of the whole page are read once.
     *
     * @param page zero-based page number, at least 0
     * @param size number of Series per page, at least 1
     */
    @Transactional(readOnly = true)
    public PublicSeriesPage findPublicSeriesPage(int page, int size) {
        PublicContentPage content = publicContent.findPublicPage(
                seriesRepository.findAllContentIds(), Set.of(ContentType.SERIES), page, size);
        if (content.items().isEmpty()) {
            return PublicSeriesPage.empty();
        }

        List<UUID> seriesIds = content.items().stream().map(PublicContentSummary::id).toList();
        Map<UUID, List<TopicLabel>> topics = topicAssignments.topicLabelsOf(seriesIds);
        List<PublicSeriesPage.Entry> entries = content.items().stream()
                .map(series -> new PublicSeriesPage.Entry(
                        series, topics.getOrDefault(series.id(), List.of())))
                .toList();
        return new PublicSeriesPage(entries, content.hasMore());
    }
}
