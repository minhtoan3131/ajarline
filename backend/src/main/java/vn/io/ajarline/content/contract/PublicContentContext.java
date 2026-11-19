package vn.io.ajarline.content.contract;

/**
 * Holds the public parents of an item. A parent that readers cannot see is always null.
 * An Article can have a Module and a Series. A Module can have a Series. A Series has neither.
 */
public record PublicContentContext(PublicSeriesRef series, PublicModuleRef module) {

    public static final PublicContentContext EMPTY = new PublicContentContext(null, null);
}
