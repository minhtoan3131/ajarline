package vn.io.ajarline.learning.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicationStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.io.ajarline.content.contract.PublicationStatus.DRAFT;
import static vn.io.ajarline.content.contract.PublicationStatus.PUBLISHED;
import static vn.io.ajarline.content.contract.PublicationStatus.UNPUBLISHED;
import static vn.io.ajarline.support.TopicTestData.uuid;

class PublicHierarchyPolicyTest {

    private static final UUID SERIES = uuid(1);
    private static final UUID MODULE_ONE = uuid(11);
    private static final UUID MODULE_TWO = uuid(12);
    private static final UUID MODULE_THREE = uuid(13);
    private static final UUID FIRST = uuid(21);
    private static final UUID SECOND = uuid(22);
    private static final UUID THIRD = uuid(23);

    private final PublicHierarchyPolicy policy = new PublicHierarchyPolicy();

    @Test
    void judge_ShouldMakeModulePublic_WhenItIsPublishedAndHasPublicArticle() {
        HierarchyFacts facts = new Facts()
                .module(MODULE_ONE, PUBLISHED, FIRST, SECOND)
                .article(FIRST, DRAFT)
                .article(SECOND, PUBLISHED)
                .build();

        PublicHierarchy hierarchy = policy.judge(facts);

        assertThat(hierarchy.isPublic(MODULE_ONE)).isTrue();
        assertThat(hierarchy.publicChildrenOf(MODULE_ONE)).containsExactly(SECOND);
    }

    @ParameterizedTest(name = "[{index}] a {0} Module is hidden")
    @EnumSource(value = PublicationStatus.class, names = {"DRAFT", "UNPUBLISHED"})
    void judge_ShouldHideModule_WhenItHasPublicArticleButIsNotPublished(PublicationStatus status) {
        HierarchyFacts facts = new Facts()
                .module(MODULE_ONE, status, FIRST)
                .article(FIRST, PUBLISHED)
                .build();

        PublicHierarchy hierarchy = policy.judge(facts);

        assertThat(hierarchy.isPublic(MODULE_ONE)).isFalse();
        assertThat(hierarchy.publicChildrenOf(MODULE_ONE)).isEmpty();
    }

    @Test
    void judge_ShouldHideModule_WhenItIsPublishedButHasNoPublicArticle() {
        HierarchyFacts facts = new Facts()
                .module(MODULE_ONE, PUBLISHED, FIRST, SECOND)
                .article(FIRST, DRAFT)
                .article(SECOND, UNPUBLISHED)
                .module(MODULE_TWO, PUBLISHED)
                .build();

        PublicHierarchy hierarchy = policy.judge(facts);

        assertThat(hierarchy.isPublic(MODULE_ONE)).isFalse();
        assertThat(hierarchy.isPublic(MODULE_TWO)).isFalse();
    }

    @Test
    void judge_ShouldMakeSeriesPublic_WhenItIsPublishedAndHasPublicModule() {
        HierarchyFacts facts = new Facts()
                .series(SERIES, PUBLISHED, MODULE_ONE, MODULE_TWO)
                .module(MODULE_ONE, DRAFT, FIRST)
                .module(MODULE_TWO, PUBLISHED, SECOND)
                .article(FIRST, PUBLISHED)
                .article(SECOND, PUBLISHED)
                .build();

        PublicHierarchy hierarchy = policy.judge(facts);

        assertThat(hierarchy.isPublic(SERIES)).isTrue();
        assertThat(hierarchy.publicChildrenOf(SERIES)).containsExactly(MODULE_TWO);
    }

    @Test
    void judge_ShouldHideSeries_WhenItsPublishedModuleHasNoPublicArticle() {
        HierarchyFacts facts = new Facts()
                .series(SERIES, PUBLISHED, MODULE_ONE)
                .module(MODULE_ONE, PUBLISHED, FIRST)
                .article(FIRST, DRAFT)
                .build();

        PublicHierarchy hierarchy = policy.judge(facts);

        assertThat(hierarchy.isPublic(SERIES)).isFalse();
    }

    @Test
    void judge_ShouldHideSeries_WhenItHasNoModule() {
        HierarchyFacts facts = new Facts().series(SERIES, PUBLISHED).build();

        PublicHierarchy hierarchy = policy.judge(facts);

        assertThat(hierarchy.isPublic(SERIES)).isFalse();
    }

    @Test
    void judge_ShouldKeepModuleRank_WhenItsSeriesIsNotPublished() {
        HierarchyFacts facts = new Facts()
                .series(SERIES, DRAFT, MODULE_ONE)
                .module(MODULE_ONE, PUBLISHED, FIRST)
                .article(FIRST, PUBLISHED)
                .build();

        PublicHierarchy hierarchy = policy.judge(facts);

        assertThat(hierarchy.isPublic(SERIES)).isFalse();
        assertThat(hierarchy.isPublic(MODULE_ONE)).isTrue();
        assertThat(hierarchy.publicParentOf(MODULE_ONE)).isEmpty();
        assertThat(hierarchy.parentOf(MODULE_ONE)).contains(SERIES);
        assertThat(policy.rankedChildren(hierarchy, SERIES))
                .containsExactly(new PublicHierarchyPolicy.RankedChild(MODULE_ONE, 1));
    }

    @Test
    void judge_ShouldNeverGiveEmptyChildList_WhenAParentIsPublic() {
        HierarchyFacts facts = new Facts()
                .series(SERIES, PUBLISHED, MODULE_ONE, MODULE_TWO, MODULE_THREE)
                .module(MODULE_ONE, PUBLISHED)
                .module(MODULE_TWO, PUBLISHED, FIRST)
                .module(MODULE_THREE, DRAFT, SECOND)
                .article(FIRST, PUBLISHED)
                .article(SECOND, PUBLISHED)
                .build();

        PublicHierarchy hierarchy = policy.judge(facts);

        assertThat(hierarchy.publicIds()).containsExactlyInAnyOrder(SERIES, MODULE_TWO);
        hierarchy.publicIds().forEach(id ->
                assertThat(hierarchy.publicChildrenOf(id)).isNotEmpty());
    }

    @Test
    void publicParentOf_ShouldBeEmpty_WhenArticleModuleIsHidden() {
        HierarchyFacts facts = new Facts()
                .module(MODULE_ONE, DRAFT, FIRST)
                .article(FIRST, PUBLISHED)
                .build();

        PublicHierarchy hierarchy = policy.judge(facts);

        assertThat(hierarchy.publicParentOf(FIRST)).isEmpty();
        assertThat(hierarchy.parentOf(FIRST)).contains(MODULE_ONE);
    }

    @Test
    void rankedChildren_ShouldNumberOnlyPublicChildren_WhenMiddleChildIsHidden() {
        PublicHierarchy hierarchy = policy.judge(moduleWithThreeArticles(PUBLISHED, DRAFT, PUBLISHED));

        List<PublicHierarchyPolicy.RankedChild> ranked = policy.rankedChildren(hierarchy, MODULE_ONE);

        assertThat(ranked).containsExactly(
                new PublicHierarchyPolicy.RankedChild(FIRST, 1),
                new PublicHierarchyPolicy.RankedChild(THIRD, 2));
    }

    @Test
    void siblingsOf_ShouldHaveNoPrevious_WhenArticleIsFirst() {
        PublicHierarchy hierarchy = policy.judge(moduleWithThreeArticles(PUBLISHED, PUBLISHED, PUBLISHED));

        PublicHierarchyPolicy.Siblings siblings =
                policy.siblingsOf(hierarchy, MODULE_ONE, FIRST).orElseThrow();

        assertThat(siblings.previous()).isNull();
        assertThat(siblings.self().position()).isEqualTo(1);
        assertThat(siblings.next().id()).isEqualTo(SECOND);
        assertThat(siblings.next().position()).isEqualTo(2);
    }

    @Test
    void siblingsOf_ShouldHaveNoNext_WhenArticleIsLast() {
        PublicHierarchy hierarchy = policy.judge(moduleWithThreeArticles(PUBLISHED, PUBLISHED, PUBLISHED));

        PublicHierarchyPolicy.Siblings siblings =
                policy.siblingsOf(hierarchy, MODULE_ONE, THIRD).orElseThrow();

        assertThat(siblings.previous().id()).isEqualTo(SECOND);
        assertThat(siblings.next()).isNull();
        assertThat(siblings.self().position()).isEqualTo(3);
    }

    @Test
    void siblingsOf_ShouldHaveNoNeighbour_WhenArticleIsAlone() {
        HierarchyFacts facts = new Facts()
                .module(MODULE_ONE, PUBLISHED, FIRST)
                .article(FIRST, PUBLISHED)
                .build();
        PublicHierarchy hierarchy = policy.judge(facts);

        PublicHierarchyPolicy.Siblings siblings =
                policy.siblingsOf(hierarchy, MODULE_ONE, FIRST).orElseThrow();

        assertThat(siblings.previous()).isNull();
        assertThat(siblings.next()).isNull();
    }

    @Test
    void siblingsOf_ShouldSkipHiddenNeighbour_WhenMiddleArticleIsDraft() {
        PublicHierarchy hierarchy = policy.judge(moduleWithThreeArticles(PUBLISHED, DRAFT, PUBLISHED));

        PublicHierarchyPolicy.Siblings first =
                policy.siblingsOf(hierarchy, MODULE_ONE, FIRST).orElseThrow();
        PublicHierarchyPolicy.Siblings third =
                policy.siblingsOf(hierarchy, MODULE_ONE, THIRD).orElseThrow();

        assertThat(first.next().id()).isEqualTo(THIRD);
        assertThat(first.next().position()).isEqualTo(2);
        assertThat(third.previous().id()).isEqualTo(FIRST);
    }

    @Test
    void siblingsOf_ShouldBeEmpty_WhenArticleIsHidden() {
        PublicHierarchy hierarchy = policy.judge(moduleWithThreeArticles(PUBLISHED, DRAFT, PUBLISHED));

        assertThat(policy.siblingsOf(hierarchy, MODULE_ONE, SECOND)).isEmpty();
    }

    private static HierarchyFacts moduleWithThreeArticles(
            PublicationStatus first, PublicationStatus second, PublicationStatus third) {
        return new Facts()
                .module(MODULE_ONE, PUBLISHED, FIRST, SECOND, THIRD)
                .article(FIRST, first)
                .article(SECOND, second)
                .article(THIRD, third)
                .build();
    }

    /**
     * Builds facts by hand, so a test shows the whole situation in a few lines.
     */
    private static final class Facts {
        private final Map<UUID, HierarchyFacts.Fact> byId = new HashMap<>();

        Facts series(UUID id, PublicationStatus status, UUID... moduleIds) {
            byId.put(id, new HierarchyFacts.Fact(ContentType.SERIES, status, List.of(moduleIds)));
            return this;
        }

        Facts module(UUID id, PublicationStatus status, UUID... articleIds) {
            byId.put(id, new HierarchyFacts.Fact(ContentType.MODULE, status, List.of(articleIds)));
            return this;
        }

        Facts article(UUID id, PublicationStatus status) {
            byId.put(id, new HierarchyFacts.Fact(ContentType.ARTICLE, status, List.of()));
            return this;
        }

        HierarchyFacts build() {
            return new HierarchyFacts(byId);
        }
    }
}
