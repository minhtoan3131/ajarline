package vn.io.ajarline.learning.contract;

import vn.io.ajarline.content.contract.ContentRecordInput;

import java.util.UUID;

/**
 * Editable state of a Module, with its Series and position.
 * The position must be {@code 1..N+1}, where N is the number of other Modules in that Series.
 */
public record ModuleContentInput(
        ContentRecordInput record,
        String overviewMarkdown,
        UUID seriesId,
        int position
) {
}
