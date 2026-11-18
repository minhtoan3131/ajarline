package vn.io.ajarline.learning.contract;

import java.util.UUID;

/**
 * Where one child sits after a reorder, with its revision after the reorder.
 */
public record ChildPosition(UUID id, int position, long revision) {
}
