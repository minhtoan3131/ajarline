package vn.io.ajarline.content.contract;

/**
 * Holds the complete Article content needed to create or update an Article.
 *
 * <p>This is an immutable input object, not a database entity or an HTTP request.
 * It contains only the content managed by the content capability,
 * without Module or Topic relationships.</p>
 */
public record ArticleContentInput(
        String title,
        String slug,
        String summary,
        String bodyMarkdown
) {
}
