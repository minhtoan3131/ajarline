package vn.io.ajarline.content.contract;

/**
 * Editable state of a News item. The common fields stay in one nested record.
 */
public record NewsContentInput(
        ContentRecordInput record,
        String bodyMarkdown,
        NewsSource source
) {
}
