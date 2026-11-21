package vn.io.ajarline.learning.contract;

/**
 * Stable failure codes of the learning capability.
 * The Admin API maps each name to an HTTP status, so a rename is a breaking change.
 */
public enum LearningFailureCode {
    VALIDATION_FAILED,
    INVALID_POSITION,
    ORDER_SET_CONFLICT,
    SERIES_OVERVIEW_REQUIRED,
    MODULE_OVERVIEW_REQUIRED,
    SERIES_REFERENCE_NOT_FOUND,
    MODULE_REFERENCE_NOT_FOUND,
    CONTENT_IN_USE
}
