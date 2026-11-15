package vn.io.ajarline.content.contract;

import java.util.List;
import java.util.UUID;

/**
 * Holds the complete Article content needed to create or update an Article.
 *
 * <p>This is an immutable input object, not a database entity or an HTTP request.
 * It carries the text fields and the ids of the Topics the Article should have.
 * Module membership is not part of it.</p>
 */
public record ArticleContentInput(
        String title,
        String slug,
        String summary,
        String bodyMarkdown,
        List<UUID> topicIds
) {

    public ArticleContentInput {
        topicIds = List.copyOf(topicIds);
    }

    /** Creates an input for an Article that has no Topics. */
    public ArticleContentInput(String title, String slug, String summary, String bodyMarkdown) {
        this(title, slug, summary, bodyMarkdown, List.of());
    }
}
