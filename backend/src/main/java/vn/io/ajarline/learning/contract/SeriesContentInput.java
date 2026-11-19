package vn.io.ajarline.learning.contract;

import vn.io.ajarline.content.contract.ContentRecordInput;

/**
 * Editable state of a Series. The common fields stay in one nested record.
 */
public record SeriesContentInput(
        ContentRecordInput record,
        String overviewMarkdown
) {
}
