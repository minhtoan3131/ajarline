package vn.io.ajarline.publication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vn.io.ajarline.content.contract.PublicationStatus.DRAFT;
import static vn.io.ajarline.content.contract.PublicationStatus.PUBLISHED;
import static vn.io.ajarline.content.contract.PublicationStatus.UNPUBLISHED;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.io.ajarline.publication.contract.PublicationFailureCode;
import vn.io.ajarline.publication.contract.PublicationOperationException;

class StructurePublicationPolicyTest {

    private final StructurePublicationPolicy policy = new StructurePublicationPolicy();

    @Test
    void requireModulePublishable_ShouldPass_WhenOneArticleIsPublished() {
        assertThatCode(() -> policy.requireModulePublishable(List.of(DRAFT, PUBLISHED, UNPUBLISHED)))
                .doesNotThrowAnyException();
    }

    @Test
    void requireModulePublishable_ShouldReject_WhenModuleHasNoArticle() {
        assertThatThrownBy(() -> policy.requireModulePublishable(List.of()))
                .isInstanceOfSatisfying(PublicationOperationException.class, failure ->
                        assertThat(failure.code())
                                .isEqualTo(PublicationFailureCode.MODULE_HAS_NO_PUBLIC_ARTICLE));
    }

    @Test
    void requireModulePublishable_ShouldReject_WhenNoArticleIsPublished() {
        assertThatThrownBy(() -> policy.requireModulePublishable(List.of(DRAFT, UNPUBLISHED)))
                .isInstanceOfSatisfying(PublicationOperationException.class, failure ->
                        assertThat(failure.code())
                                .isEqualTo(PublicationFailureCode.MODULE_HAS_NO_PUBLIC_ARTICLE));
    }

    @Test
    void requireSeriesPublishable_ShouldPass_WhenOneModuleIsPublishedWithPublicArticle() {
        List<ModuleCandidate> modules = List.of(
                new ModuleCandidate(DRAFT, false),
                new ModuleCandidate(PUBLISHED, true));

        assertThatCode(() -> policy.requireSeriesPublishable(modules)).doesNotThrowAnyException();
    }

    @Test
    void requireSeriesPublishable_ShouldReject_WhenSeriesHasNoModule() {
        assertSeriesRejected(List.of());
    }

    @Test
    void requireSeriesPublishable_ShouldReject_WhenPublishedModuleHasNoPublicArticle() {
        assertSeriesRejected(List.of(new ModuleCandidate(PUBLISHED, false)));
    }

    @Test
    void requireSeriesPublishable_ShouldReject_WhenModuleWithPublicArticleIsNotPublished() {
        assertSeriesRejected(List.of(
                new ModuleCandidate(DRAFT, true),
                new ModuleCandidate(UNPUBLISHED, true)));
    }

    private void assertSeriesRejected(List<ModuleCandidate> modules) {
        assertThatThrownBy(() -> policy.requireSeriesPublishable(modules))
                .isInstanceOfSatisfying(PublicationOperationException.class, failure ->
                        assertThat(failure.code())
                                .isEqualTo(PublicationFailureCode.SERIES_HAS_NO_PUBLIC_MODULE));
    }
}
