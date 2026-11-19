package vn.io.ajarline.learning.application;

import org.springframework.stereotype.Component;
import vn.io.ajarline.learning.contract.LearningFailureCode;
import vn.io.ajarline.learning.contract.LearningOperationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Computes child positions inside one parent so they stay a continuous sequence {@code 1..N}.
 *
 * <p>The policy does no I/O. It returns only the positions that change. The caller must lock the
 * parent before it reads the children, because this class cannot check that.</p>
 *
 * <p>N does not count the child that is being placed. A valid position is {@code 1..N+1}.</p>
 */
@Component
public class PositionPolicy {

    /**
     * Inserts a child that is not in the parent yet. {@code currentIds} must not contain it.
     * Returns the child and every sibling that moves down.
     *
     * @throws LearningOperationException with {@code INVALID_POSITION} outside {@code 1..N+1}
     */
    public List<PositionAssignment> insert(List<UUID> currentIds, UUID childId, int position) {
        if (currentIds.contains(childId)) {
            throw new IllegalArgumentException("The child is already in the parent: " + childId);
        }
        requirePosition(position, currentIds.size() + 1);

        List<PositionAssignment> assignments = new ArrayList<>();
        assignments.add(new PositionAssignment(childId, position));
        // A sibling at index i has position i + 1. From the target index on, it moves down by one.
        for (int index = position - 1; index < currentIds.size(); index++) {
            assignments.add(new PositionAssignment(currentIds.get(index), index + 2));
        }
        return List.copyOf(assignments);
    }

    /**
     * Takes a child out of a parent. Returns the siblings after it, each moved up by one.
     * The child is not in the result. The caller decides where it goes.
     */
    public List<PositionAssignment> detach(List<UUID> currentIds, UUID childId) {
        int childIndex = currentIds.indexOf(childId);
        if (childIndex < 0) {
            throw new IllegalArgumentException("The child is not in the parent: " + childId);
        }

        List<PositionAssignment> assignments = new ArrayList<>();
        // The sibling at index i (position i + 1) moves up to position i.
        for (int index = childIndex + 1; index < currentIds.size(); index++) {
            assignments.add(new PositionAssignment(currentIds.get(index), index));
        }
        return List.copyOf(assignments);
    }

    /**
     * Moves a child inside the parent it is already in. {@code currentIds} must contain it.
     * Returns only the children whose position changes.
     *
     * @throws LearningOperationException with {@code INVALID_POSITION} outside {@code 1..N+1}
     */
    public List<PositionAssignment> move(List<UUID> currentIds, UUID childId, int position) {
        List<UUID> reordered = new ArrayList<>(currentIds);
        if (!reordered.remove(childId)) {
            throw new IllegalArgumentException("The child is not in the parent: " + childId);
        }
        requirePosition(position, reordered.size() + 1);
        reordered.add(position - 1, childId);
        return changedPositions(currentIds, reordered);
    }

    /**
     * Replaces the whole order of a parent. Returns only the children whose position changes.
     *
     * @throws LearningOperationException with {@code ORDER_SET_CONFLICT} when the requested
     *         ids are not exactly the current children
     */
    public List<PositionAssignment> reorder(List<UUID> currentIds, List<UUID> requestedIds) {
        boolean sameSet = requestedIds.size() == currentIds.size()
                && new HashSet<>(requestedIds).equals(new HashSet<>(currentIds));
        if (!sameSet) {
            throw new LearningOperationException(
                    LearningFailureCode.ORDER_SET_CONFLICT,
                    "The ids must be exactly the current children."
            );
        }
        return changedPositions(currentIds, requestedIds);
    }

    /**
     * Rejects a list with a repeated id. It needs no database, so callers can run it before any lock.
     */
    public void requireDistinct(List<UUID> ids) {
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new LearningOperationException(
                    LearningFailureCode.VALIDATION_FAILED,
                    "Ids must not repeat."
            );
        }
    }

    private void requirePosition(int position, int highestValid) {
        if (position < 1 || position > highestValid) {
            throw new LearningOperationException(
                    LearningFailureCode.INVALID_POSITION,
                    "Position must be between 1 and " + highestValid + "."
            );
        }
    }

    /**
     * Both lists hold the same ids. An id with a different index has a new position.
     */
    private List<PositionAssignment> changedPositions(List<UUID> before, List<UUID> after) {
        List<PositionAssignment> changes = new ArrayList<>();
        for (int index = 0; index < after.size(); index++) {
            if (!after.get(index).equals(before.get(index))) {
                changes.add(new PositionAssignment(after.get(index), index + 1));
            }
        }
        return List.copyOf(changes);
    }
}
