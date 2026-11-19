package vn.io.ajarline.learning.application;

import org.springframework.stereotype.Component;
import vn.io.ajarline.content.contract.ContentRecordReader;
import vn.io.ajarline.content.contract.ContentRecordSummary;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleArticleEntity;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleArticleJpaRepository;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleEntity;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleJpaRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Reads Series, Modules and Articles in batches, for any number of ids.
 * It takes no lock and decides nothing. The caller owns the transaction.
 */
@Component
public class PublicHierarchyReader {
    private final ContentRecordReader records;
    private final ModuleJpaRepository moduleRepository;
    private final ModuleArticleJpaRepository memberRepository;

    public PublicHierarchyReader(ContentRecordReader records,
                                 ModuleJpaRepository moduleRepository,
                                 ModuleArticleJpaRepository memberRepository) {
        this.records = records;
        this.moduleRepository = moduleRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * Reads the given Series and Modules and everything below them. An id that matches no item is ignored.
     * The children of an item are read only when the item is published.
     */
    public HierarchyFacts readDown(Collection<UUID> ids) {
        Map<UUID, ContentRecordSummary> known = new HashMap<>(records.summariesOf(ids));
        return assemble(known,
                idsWhere(known, ContentType.SERIES, PublicationStatus.PUBLISHED),
                idsWhere(known, ContentType.MODULE, PublicationStatus.PUBLISHED));
    }

    /**
     * Reads the given Articles and Modules, the Modules and Series above them, and everything
     * below those Series. A Series is read even when it is not published, because a Module
     * under it needs a rank. An id that matches no item is ignored.
     */
    public HierarchyFacts readAround(Collection<UUID> ids) {
        Map<UUID, ContentRecordSummary> known = new HashMap<>(records.summariesOf(ids));

        // Up from an Article to its Module.
        Set<UUID> moduleIds = new LinkedHashSet<>(idsOfType(known, ContentType.MODULE));
        memberRepository.findAllById(idsOfType(known, ContentType.ARTICLE))
                .forEach(member -> moduleIds.add(member.getModuleId()));
        learn(known, moduleIds);

        // Up from a Module to its Series.
        Set<UUID> seriesIds = new LinkedHashSet<>();
        moduleRepository.findAllById(moduleIds)
                .forEach(module -> seriesIds.add(module.getSeriesId()));
        learn(known, seriesIds);

        return assemble(known, seriesIds, moduleIds);
    }

    private static List<UUID> idsOfType(Map<UUID, ContentRecordSummary> known, ContentType type) {
        return known.values().stream()
                .filter(summary -> summary.type() == type)
                .map(ContentRecordSummary::id)
                .toList();
    }

    /**
     * Reads the children of the given Series and Modules, then the Articles of every published Module.
     * Every Module and Series in {@code known} gets its status read first.
     */
    private HierarchyFacts assemble(Map<UUID, ContentRecordSummary> known,
                                    Collection<UUID> seriesToOpen,
                                    Collection<UUID> modulesToOpen) {
        Map<UUID, List<UUID>> modulesOfSeries = modulesOf(seriesToOpen);
        learn(known, modulesOfSeries.values().stream().flatMap(List::stream).toList());

        // Only a published Module can have a public Article, so only its Articles are read.
        Set<UUID> candidateModules = new LinkedHashSet<>(modulesToOpen);
        modulesOfSeries.values().forEach(candidateModules::addAll);
        List<UUID> publishedModules = candidateModules.stream()
                .filter(id -> known.get(id).status() == PublicationStatus.PUBLISHED)
                .toList();
        Map<UUID, List<UUID>> articlesOfModule = articlesOf(publishedModules);
        learn(known, articlesOfModule.values().stream().flatMap(List::stream).toList());

        Map<UUID, HierarchyFacts.Fact> facts = new HashMap<>();
        known.forEach((id, summary) -> {
            List<UUID> children = switch (summary.type()) {
                case SERIES -> modulesOfSeries.getOrDefault(id, List.of());
                case MODULE -> articlesOfModule.getOrDefault(id, List.of());
                case ARTICLE -> List.of();
            };
            facts.put(id, new HierarchyFacts.Fact(summary.type(), summary.status(), children));
        });
        return new HierarchyFacts(facts);
    }

    private Map<UUID, List<UUID>> modulesOf(Collection<UUID> seriesIds) {
        if (seriesIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<UUID>> modules = new HashMap<>();
        for (ModuleEntity module : moduleRepository.findBySeriesIdInOrderBySeriesIdAscPositionAsc(seriesIds)) {
            modules.computeIfAbsent(module.getSeriesId(), key -> new ArrayList<>())
                    .add(module.getContentId());
        }
        return modules;
    }

    private Map<UUID, List<UUID>> articlesOf(Collection<UUID> moduleIds) {
        if (moduleIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<UUID>> articles = new HashMap<>();
        for (ModuleArticleEntity member
                : memberRepository.findByModuleIdInOrderByModuleIdAscPositionAsc(moduleIds)) {
            articles.computeIfAbsent(member.getModuleId(), key -> new ArrayList<>())
                    .add(member.getArticleId());
        }
        return articles;
    }

    /**
     * Reads the common record of the ids that are not known yet, with one query.
     */
    private void learn(Map<UUID, ContentRecordSummary> known, Collection<UUID> ids) {
        List<UUID> missing = ids.stream().filter(id -> !known.containsKey(id)).distinct().toList();
        known.putAll(records.summariesOf(missing));
    }

    private static List<UUID> idsWhere(Map<UUID, ContentRecordSummary> known,
                                       ContentType type, PublicationStatus status) {
        return known.values().stream()
                .filter(summary -> summary.type() == type && summary.status() == status)
                .map(ContentRecordSummary::id)
                .toList();
    }
}
