package vn.io.ajarline.publication.contract;

/**
 * Stable failure codes of the publication capability.
 * The Admin API maps each name to an HTTP status, so a rename is a breaking change.
 */
public enum PublicationFailureCode {
    INVALID_PUBLICATION_STATE,
    MODULE_HAS_NO_PUBLIC_ARTICLE,
    SERIES_HAS_NO_PUBLIC_MODULE
}
