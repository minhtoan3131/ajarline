package vn.io.ajarline.publication.application;

import vn.io.ajarline.content.contract.PublicationStatus;

/**
 * What the publication rules need to know about one Module of a Series.
 */
public record ModuleCandidate(PublicationStatus status, boolean hasPublicArticle) {
}
