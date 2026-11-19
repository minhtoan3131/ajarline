package vn.io.ajarline.learning.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.io.ajarline.learning.contract.LearningFailureCode;
import vn.io.ajarline.learning.contract.LearningOperationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LearningContentPolicyTest {

    private final LearningContentPolicy policy = new LearningContentPolicy();

    @ParameterizedTest(name = "[{index}] rejects \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void requirePublishableSeriesOverview_ShouldRejectMissingOverview(String overview) {
        assertThatThrownBy(() -> policy.requirePublishableSeriesOverview(overview))
                .isInstanceOfSatisfying(LearningOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(LearningFailureCode.SERIES_OVERVIEW_REQUIRED));
    }

    @Test
    void requirePublishableSeriesOverview_ShouldAcceptPresentOverview() {
        assertThatCode(() -> policy.requirePublishableSeriesOverview("# Overview"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] rejects \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void requirePublishableModuleOverview_ShouldRejectMissingOverview(String overview) {
        assertThatThrownBy(() -> policy.requirePublishableModuleOverview(overview))
                .isInstanceOfSatisfying(LearningOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(LearningFailureCode.MODULE_OVERVIEW_REQUIRED));
    }

    @Test
    void requirePublishableModuleOverview_ShouldAcceptPresentOverview() {
        assertThatCode(() -> policy.requirePublishableModuleOverview("# Overview"))
                .doesNotThrowAnyException();
    }
}
