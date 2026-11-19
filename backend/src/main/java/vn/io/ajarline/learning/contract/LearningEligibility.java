package vn.io.ajarline.learning.contract;

import java.util.UUID;

/**
 * Tells the publication workflow whether the fields that only learning content has are ready.
 * Every method runs in the caller's transaction.
 */
public interface LearningEligibility {

    /**
     * @throws LearningOperationException with {@code SERIES_OVERVIEW_REQUIRED}
     *         when the overview of the Series is missing or blank
     */
    void requirePublishableSeries(UUID id);

    /**
     * @throws LearningOperationException with {@code MODULE_OVERVIEW_REQUIRED}
     *         when the overview of the Module is missing or blank
     */
    void requirePublishableModule(UUID id);
}
