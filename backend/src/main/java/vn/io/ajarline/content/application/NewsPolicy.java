package vn.io.ajarline.content.application;

import org.springframework.stereotype.Component;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;

@Component
public class NewsPolicy {
    private final ContentRecordPolicy records;
    private final SourceUrlPolicy urls;

    public NewsPolicy(ContentRecordPolicy records, SourceUrlPolicy urls) {
        this.records = records;
        this.urls = urls;
    }

    /**
     * Checks that a News item is complete enough to be public. It reports the first missing
     * part, in this order: summary, body, source.
     *
     * @throws ContentOperationException with {@code CONTENT_SUMMARY_REQUIRED},
     *         {@code NEWS_BODY_REQUIRED} or {@code NEWS_SOURCE_REQUIRED}
     */
    public void requirePublishable(
            String summary, String bodyMarkdown, String sourceName, String sourceUrl) {
        records.requirePublishableSummary(summary);
        if (bodyMarkdown == null || bodyMarkdown.isBlank()) {
            throw failure(ContentFailureCode.NEWS_BODY_REQUIRED,
                    "A body is required for a published News item.");
        }
        if (sourceName == null || sourceName.isBlank() || !urls.isValid(sourceUrl)) {
            throw failure(ContentFailureCode.NEWS_SOURCE_REQUIRED,
                    "A source name and a valid source URL are required for a published News item.");
        }
    }

    private ContentOperationException failure(ContentFailureCode code, String message) {
        return new ContentOperationException(code, message);
    }
}
