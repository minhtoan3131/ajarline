package vn.io.ajarline.content.application;

import org.springframework.stereotype.Component;
import vn.io.ajarline.content.contract.ArticleContentInput;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;


@Component
public class ArticlePolicy {
    private final ContentRecordPolicy records;

    public ArticlePolicy(ContentRecordPolicy records) {
        this.records = records;
    }

    /**
     * Returns canonical input accepted by Article content operations.
     *
     * @throws ContentOperationException when an input field violates the
     *         content contract
     */
    public ArticleContentInput normalizeAndValidate(ArticleContentInput input) {
        if (input == null) {
            throw failure(ContentFailureCode.VALIDATION_FAILED, "Article content is required.");
        }
        records.requireValidTitle(input.title());
        String slug = records.normalizeSlug(input.slug());
        String summary = records.normalizeSummary(input.summary());
        records.requireDistinctTopicIds(input.topicIds());
        return new ArticleContentInput(input.title(), slug, summary, input.bodyMarkdown(), input.topicIds());
    }

    public void requirePublishable(String summary, String bodyMarkdown) {
        records.requirePublishableSummary(summary);
        if (bodyMarkdown == null || bodyMarkdown.isBlank()) {
            throw failure(ContentFailureCode.ARTICLE_BODY_REQUIRED,
                    "A body is required for a published Article.");
        }
    }

    private ContentOperationException failure(ContentFailureCode code, String message) {
        return new ContentOperationException(code, message);
    }
}
