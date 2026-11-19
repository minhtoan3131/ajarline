package vn.io.ajarline.content.contract;

import java.util.Objects;

/**
 * Holds what readers can see around one Article: its Module and Series, and the nearest
 * public Articles before and after it in the same Module. A missing neighbour is null.
 * An Article whose Module is hidden has no context at all.
 */
public record PublicArticleContext(
        PublicContentContext context,
        PublicArticleRef previous,
        PublicArticleRef next
) {

    public static final PublicArticleContext EMPTY =
            new PublicArticleContext(PublicContentContext.EMPTY, null, null);

    public PublicArticleContext {
        Objects.requireNonNull(context, "context");
    }
}
