package vn.io.ajarline.content.application;

import org.springframework.stereotype.Component;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.content.contract.NewsSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

    /**
     * Returns the source in canonical form: parts trimmed, an empty part as null.
     * A draft may have an empty source, so a missing name or URL is not an error here.
     *
     * @throws ContentOperationException with {@code VALIDATION_FAILED} when the name or the URL
     *         is too long, or the URL is not a safe absolute http or https URL
     */
    public NewsSource normalizeSource(NewsSource source) {
        if (source == null) {
            throw failure(ContentFailureCode.VALIDATION_FAILED, "Source is required.");
        }
        String name = blankToNull(source.name());
        String url = blankToNull(source.url());
        requireMaxLength(name, 300, "Source name");
        requireMaxLength(url, 2048, "Source URL");
        if (url != null && !urls.isValid(url)) {
            throw failure(ContentFailureCode.VALIDATION_FAILED,
                    "Source URL must be an absolute http or https URL without spaces.");
        }
        // The database keeps microseconds. A longer time would look changed on every save.
        return new NewsSource(name, url, truncate(source.publishedAt()));
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    private static Instant truncate(Instant time) {
        return time == null ? null : time.truncatedTo(ChronoUnit.MICROS);
    }

    private void requireMaxLength(String value, int maximum, String fieldName) {
        if (value != null && value.codePointCount(0, value.length()) > maximum) {
            throw failure(ContentFailureCode.VALIDATION_FAILED,
                    fieldName + " must not exceed " + maximum + " characters.");
        }
    }

    private ContentOperationException failure(ContentFailureCode code, String message) {
        return new ContentOperationException(code, message);
    }
}
