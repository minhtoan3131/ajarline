package vn.io.ajarline.learning.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentRecordReader;
import vn.io.ajarline.content.contract.ContentRecordSummary;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicContentContext;
import vn.io.ajarline.content.contract.PublicContentPage;
import vn.io.ajarline.content.contract.PublicContentQuery;
import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.content.contract.PublicSeriesRef;
import vn.io.ajarline.content.contract.TopicAssignmentPort;
import vn.io.ajarline.content.contract.TopicLabel;
import vn.io.ajarline.learning.application.PublicHierarchyPolicy.RankedChild;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleEntity;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleJpaRepository;
import vn.io.ajarline.learning.infrastructure.persistence.SeriesEntity;
import vn.io.ajarline.learning.infrastructure.persistence.SeriesJpaRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Runs the anonymous reads of Series and Modules.
 *
 * <p>An item that readers cannot see gives an empty result, whatever the reason. Nothing
 * about it is read before this is known. Content decides which Series are public in a
 * list, and cuts the page. Learning decides which children of one Series or Module are public.</p>
 */
@Service
public class PublicLearningService {
    private final ContentRecordReader records;
    private final PublicContentQuery publicContent;
    private final TopicAssignmentPort topicAssignments;
    private final PublicHierarchyReader reader;
    private final PublicHierarchyPolicy policy;
    private final SeriesJpaRepository seriesRepository;
    private final ModuleJpaRepository moduleRepository;

    public PublicLearningService(ContentRecordReader records,
                                 PublicContentQuery publicContent,
                                 TopicAssignmentPort topicAssignments,
                                 PublicHierarchyReader reader,
                                 PublicHierarchyPolicy policy,
                                 SeriesJpaRepository seriesRepository,
                                 ModuleJpaRepository moduleRepository) {
        this.records = records;
        this.publicContent = publicContent;
        this.topicAssignments = topicAssignments;
        this.reader = reader;
        this.policy = policy;
        this.seriesRepository = seriesRepository;
        this.moduleRepository = moduleRepository;
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

    /**
     * Returns a public Series with its public Modules and their public Articles, in stored order.
     * It gives nothing when no Series has this slug or readers cannot see it.
     */
    @Transactional(readOnly = true)
    public Optional<PublicSeriesView> findPublicSeries(String slug) {
        Optional<UUID> found = records.idOf(ContentType.SERIES, slug);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        UUID seriesId = found.get();

        // Nothing but the status is read until the Series is known to be public.
        PublicHierarchy hierarchy = policy.judge(reader.readDown(List.of(seriesId)));
        if (!hierarchy.isPublic(seriesId)) {
            return Optional.empty();
        }

        List<RankedChild> modules = policy.rankedChildren(hierarchy, seriesId);
        Map<UUID, List<RankedChild>> articlesOfModule = new HashMap<>();
        List<UUID> everyId = new ArrayList<>();
        everyId.add(seriesId);
        for (RankedChild module : modules) {
            everyId.add(module.id());
            List<RankedChild> articles = policy.rankedChildren(hierarchy, module.id());
            articlesOfModule.put(module.id(), articles);
            articles.forEach(article -> everyId.add(article.id()));
        }
        Map<UUID, ContentRecordSummary> texts = records.summariesOf(everyId);

        SeriesEntity series = seriesRepository.findById(seriesId).orElseThrow(
                () -> new IllegalStateException("Series subtype row is missing for " + seriesId));
        List<ModuleEntry> moduleEntries = modules.stream()
                .map(module -> new ModuleEntry(
                        toPublic(texts.get(module.id())),
                        module.position(),
                        articleEntries(articlesOfModule.get(module.id()), texts)))
                .toList();
        return Optional.of(new PublicSeriesView(
                toPublic(texts.get(seriesId)),
                requireOverview(series.getOverviewMarkdown(), seriesId),
                topicAssignments.topicLabelsOf(seriesId),
                moduleEntries));
    }

    /**
     * Returns a public Module with its public Articles, in stored order. The Series is given
     * only when readers can see it. It gives nothing when no Module has this slug or readers
     * cannot see it.
     */
    @Transactional(readOnly = true)
    public Optional<PublicModuleView> findPublicModule(String slug) {
        Optional<UUID> found = records.idOf(ContentType.MODULE, slug);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        UUID moduleId = found.get();

        PublicHierarchy hierarchy = policy.judge(reader.readAround(List.of(moduleId)));
        if (!hierarchy.isPublic(moduleId)) {
            return Optional.empty();
        }

        List<RankedChild> articles = policy.rankedChildren(hierarchy, moduleId);
        UUID seriesId = hierarchy.publicParentOf(moduleId).orElse(null);
        List<UUID> everyId = new ArrayList<>();
        everyId.add(moduleId);
        articles.forEach(article -> everyId.add(article.id()));
        if (seriesId != null) {
            everyId.add(seriesId);
        }
        Map<UUID, ContentRecordSummary> texts = records.summariesOf(everyId);

        ModuleEntity module = moduleRepository.findById(moduleId).orElseThrow(
                () -> new IllegalStateException("Module subtype row is missing for " + moduleId));
        PublicSeriesRef series = seriesId == null
                ? null
                : new PublicSeriesRef(seriesId, texts.get(seriesId).slug(), texts.get(seriesId).title());
        return Optional.of(new PublicModuleView(
                toPublic(texts.get(moduleId)),
                requireOverview(module.getOverviewMarkdown(), moduleId),
                topicAssignments.topicLabelsOf(moduleId),
                series,
                articleEntries(articles, texts)));
    }

    private static List<ArticleEntry> articleEntries(List<RankedChild> articles,
                                                     Map<UUID, ContentRecordSummary> texts) {
        return articles.stream()
                .map(article -> new ArticleEntry(toPublic(texts.get(article.id())), article.position()))
                .toList();
    }

    /**
     * Checks that a public item has what the contract requires, then drops the fields that
     * readers must not see. A missing summary means broken data, so it is not hidden.
     */
    private static PublicContentSummary toPublic(ContentRecordSummary text) {
        if (text.summary() == null || text.summary().isBlank() || text.publishedAt() == null) {
            throw new IllegalStateException(
                    "A public item has no summary or no publication time: " + text.id());
        }
        return new PublicContentSummary(
                text.id(),
                text.type(),
                text.slug(),
                text.title(),
                text.summary(),
                text.publishedAt(),
                text.updatedAt(),
                PublicContentContext.EMPTY);
    }

    private static String requireOverview(String overviewMarkdown, UUID id) {
        if (overviewMarkdown == null || overviewMarkdown.isBlank()) {
            throw new IllegalStateException("A public item has no overview: " + id);
        }
        return overviewMarkdown;
    }
}
