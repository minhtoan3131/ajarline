package vn.io.ajarline.learning.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentRecordReader;
import vn.io.ajarline.content.contract.ContentRecordSummary;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicArticleContext;
import vn.io.ajarline.content.contract.PublicArticleRef;
import vn.io.ajarline.content.contract.PublicContentContext;
import vn.io.ajarline.content.contract.PublicLearningContextPort;
import vn.io.ajarline.content.contract.PublicModuleRef;
import vn.io.ajarline.content.contract.PublicSeriesRef;
import vn.io.ajarline.learning.contract.LearningVisibility;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleJpaRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Answers what readers can see in Series and Modules. Other capabilities ask through ports.
 * Every method must run in the caller's transaction. A second transaction would show
 * two different states in one request.
 *
 * <p>It builds a reference only for an id that the policy found public, so a hidden title
 * or slug has no way out.</p>
 */
@Component
public class PublicLearningAdapter implements LearningVisibility, PublicLearningContextPort {
    private final PublicHierarchyReader reader;
    private final PublicHierarchyPolicy policy;
    private final ContentRecordReader records;
    private final ModuleJpaRepository moduleRepository;

    public PublicLearningAdapter(PublicHierarchyReader reader,
                                 PublicHierarchyPolicy policy,
                                 ContentRecordReader records,
                                 ModuleJpaRepository moduleRepository) {
        this.reader = reader;
        this.policy = policy;
        this.records = records;
        this.moduleRepository = moduleRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Set<UUID> publicModuleIds(Collection<UUID> moduleIds) {
        return publicAmong(moduleIds);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Set<UUID> publicAmong(Collection<UUID> moduleOrSeriesIds) {
        if (moduleOrSeriesIds.isEmpty()) {
            return Set.of();
        }
        PublicHierarchy hierarchy = policy.judge(reader.readDown(moduleOrSeriesIds));
        return moduleOrSeriesIds.stream()
                .filter(hierarchy::isPublic)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Map<UUID, PublicContentContext> contextsOf(Collection<UUID> articleOrModuleIds) {
        if (articleOrModuleIds.isEmpty()) {
            return Map.of();
        }
        PublicHierarchy hierarchy = policy.judge(reader.readAround(articleOrModuleIds));

        Map<UUID, Lineage> lineages = new HashMap<>();
        articleOrModuleIds.forEach(id -> lineages.put(id, lineageOf(hierarchy, id)));

        Set<UUID> textIds = new HashSet<>();
        lineages.values().forEach(lineage -> textIds.addAll(lineage.parentIds()));
        Map<UUID, ContentRecordSummary> texts = records.summariesOf(textIds);

        Map<UUID, PublicContentContext> contexts = new HashMap<>();
        lineages.forEach((id, lineage) -> contexts.put(id, contextOf(hierarchy, lineage, texts)));
        return contexts;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public PublicArticleContext articleContextOf(UUID articleId) {
        PublicHierarchy hierarchy = policy.judge(reader.readAround(List.of(articleId)));
        Lineage lineage = lineageOf(hierarchy, articleId);
        if (lineage.moduleId() == null) {
            return PublicArticleContext.EMPTY;
        }
        PublicHierarchyPolicy.Siblings siblings = policy.siblingsOf(hierarchy, lineage.moduleId(), articleId)
                .orElseThrow(() -> new IllegalStateException("A public Article has no rank: " + articleId));

        Set<UUID> textIds = lineage.parentIds();
        if (siblings.previous() != null) {
            textIds.add(siblings.previous().id());
        }
        if (siblings.next() != null) {
            textIds.add(siblings.next().id());
        }
        Map<UUID, ContentRecordSummary> texts = records.summariesOf(textIds);

        return new PublicArticleContext(
                contextOf(hierarchy, lineage, texts),
                articleRef(siblings.previous(), texts),
                articleRef(siblings.next(), texts));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Set<UUID> articleIdsInPublicModules() {
        List<UUID> moduleIds = moduleRepository.findAllContentIds();
        if (moduleIds.isEmpty()) {
            return Set.of();
        }
        PublicHierarchy hierarchy = policy.judge(reader.readDown(moduleIds));
        return moduleIds.stream()
                .filter(hierarchy::isPublic)
                .flatMap(moduleId -> hierarchy.publicChildrenOf(moduleId).stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Finds the parents that readers can see. An Article has a Module only when the Module is
     * public, and a Series only when both are public. A Module has a Series only when it is public.
     */
    private Lineage lineageOf(PublicHierarchy hierarchy, UUID id) {
        ContentType type = hierarchy.typeOf(id).orElse(null);
        if (type == ContentType.ARTICLE) {
            UUID moduleId = hierarchy.publicParentOf(id).orElse(null);
            UUID seriesId = moduleId == null ? null : hierarchy.publicParentOf(moduleId).orElse(null);
            return new Lineage(moduleId, seriesId);
        }
        if (type == ContentType.MODULE) {
            return new Lineage(null, hierarchy.publicParentOf(id).orElse(null));
        }
        return new Lineage(null, null);
    }

    private PublicContentContext contextOf(PublicHierarchy hierarchy, Lineage lineage,
                                           Map<UUID, ContentRecordSummary> texts) {
        PublicSeriesRef series = null;
        if (lineage.seriesId() != null) {
            ContentRecordSummary text = texts.get(lineage.seriesId());
            series = new PublicSeriesRef(text.id(), text.slug(), text.title());
        }
        PublicModuleRef module = null;
        if (lineage.moduleId() != null) {
            ContentRecordSummary text = texts.get(lineage.moduleId());
            module = new PublicModuleRef(
                    text.id(), text.slug(), text.title(), rankOfModule(hierarchy, text.id()));
        }
        return new PublicContentContext(series, module);
    }

    /**
     * Returns the rank of a public Module among the public Modules of its Series, even when
     * the Series itself is hidden.
     */
    private int rankOfModule(PublicHierarchy hierarchy, UUID moduleId) {
        return hierarchy.parentOf(moduleId)
                .flatMap(seriesId -> policy.siblingsOf(hierarchy, seriesId, moduleId))
                .map(siblings -> siblings.self().position())
                .orElseThrow(() -> new IllegalStateException("A public Module has no rank: " + moduleId));
    }

    private static PublicArticleRef articleRef(PublicHierarchyPolicy.RankedChild child,
                                               Map<UUID, ContentRecordSummary> texts) {
        if (child == null) {
            return null;
        }
        ContentRecordSummary text = texts.get(child.id());
        return new PublicArticleRef(text.id(), text.slug(), text.title(), child.position());
    }

    /**
     * Holds the Module and the Series that readers can see for one item. Either can be null.
     */
    private record Lineage(UUID moduleId, UUID seriesId) {

        Set<UUID> parentIds() {
            Set<UUID> ids = new HashSet<>();
            if (moduleId != null) {
                ids.add(moduleId);
            }
            if (seriesId != null) {
                ids.add(seriesId);
            }
            return ids;
        }
    }
}
