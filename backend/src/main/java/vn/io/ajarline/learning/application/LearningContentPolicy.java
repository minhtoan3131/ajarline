package vn.io.ajarline.learning.application;

import org.springframework.stereotype.Component;
import vn.io.ajarline.learning.contract.LearningFailureCode;
import vn.io.ajarline.learning.contract.LearningOperationException;

/**
 * Rules for fields that only learning content has. An overview is stored as written.
 * It is not trimmed and has no length limit of its own.
 */
@Component
public class LearningContentPolicy {

    /**
     * Rejects a missing or blank overview. Used for a published Series, because readers see it at once.
     */
    public void requirePublishableSeriesOverview(String overviewMarkdown) {
        if (overviewMarkdown == null || overviewMarkdown.isBlank()) {
            throw new LearningOperationException(
                    LearningFailureCode.SERIES_OVERVIEW_REQUIRED,
                    "An overview is required for a published Series.");
        }
    }
}
