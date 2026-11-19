package vn.io.ajarline.content.contract;

import java.util.UUID;

/**
 * Holds the public identity of a Module shown with other content.
 * The position is the rank of the Module among the public Modules of its Series, from 1.
 */
public record PublicModuleRef(UUID id, String slug, String title, int position) {
}
