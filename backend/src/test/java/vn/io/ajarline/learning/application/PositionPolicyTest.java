package vn.io.ajarline.learning.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.io.ajarline.learning.contract.LearningFailureCode;
import vn.io.ajarline.learning.contract.LearningOperationException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PositionPolicyTest {

    private static final UUID A = id(1);
    private static final UUID B = id(2);
    private static final UUID C = id(3);
    private static final UUID NEW = id(9);

    private final PositionPolicy policy = new PositionPolicy();

    @Test
    void insert_ShouldShiftEverySibling_WhenChildGoesFirst() {
        List<PositionAssignment> result = policy.insert(List.of(A, B), NEW, 1);

        assertThat(result).containsExactly(
                new PositionAssignment(NEW, 1),
                new PositionAssignment(A, 2),
                new PositionAssignment(B, 3));
    }

    @Test
    void insert_ShouldShiftOnlySiblingsAfterTheChild_WhenChildGoesInTheMiddle() {
        List<PositionAssignment> result = policy.insert(List.of(A, B, C), NEW, 2);

        assertThat(result).containsExactly(
                new PositionAssignment(NEW, 2),
                new PositionAssignment(B, 3),
                new PositionAssignment(C, 4));
    }

    @Test
    void insert_ShouldWriteOnlyTheChild_WhenChildGoesLast() {
        List<PositionAssignment> result = policy.insert(List.of(A, B), NEW, 3);

        assertThat(result).containsExactly(new PositionAssignment(NEW, 3));
    }

    @Test
    void insert_ShouldAcceptPositionOne_WhenParentHasNoChildren() {
        List<PositionAssignment> result = policy.insert(List.of(), NEW, 1);

        assertThat(result).containsExactly(new PositionAssignment(NEW, 1));
    }

    @ParameterizedTest(name = "[{index}] rejects position {0}")
    @ValueSource(ints = {-1, 0, 4})
    void insert_ShouldRejectPosition_WhenOutsideOneToNPlusOne(int position) {
        assertThatThrownBy(() -> policy.insert(List.of(A, B), NEW, position))
                .isInstanceOfSatisfying(LearningOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(LearningFailureCode.INVALID_POSITION));
    }

    @Test
    void detach_ShouldMoveEverySiblingAfterTheChildUp_WhenChildIsFirst() {
        List<PositionAssignment> result = policy.detach(List.of(A, B, C), A);

        assertThat(result).containsExactly(
                new PositionAssignment(B, 1),
                new PositionAssignment(C, 2));
    }

    @Test
    void detach_ShouldMoveOnlySiblingsAfterTheChildUp_WhenChildIsInTheMiddle() {
        List<PositionAssignment> result = policy.detach(List.of(A, B, C), B);

        assertThat(result).containsExactly(new PositionAssignment(C, 2));
    }

    @Test
    void detach_ShouldChangeNothing_WhenChildIsLast() {
        assertThat(policy.detach(List.of(A, B, C), C)).isEmpty();
    }

    @Test
    void detach_ShouldFail_WhenChildIsNotInTheParent() {
        assertThatThrownBy(() -> policy.detach(List.of(A, B), NEW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void move_ShouldReturnOnlyChangedChildren_WhenChildMovesToTheEnd() {
        List<PositionAssignment> result = policy.move(List.of(A, B, C), A, 3);

        assertThat(result).containsExactly(
                new PositionAssignment(B, 1),
                new PositionAssignment(C, 2),
                new PositionAssignment(A, 3));
    }

    @Test
    void move_ShouldReturnOnlyChangedChildren_WhenChildMovesToTheFront() {
        List<PositionAssignment> result = policy.move(List.of(A, B, C), C, 1);

        assertThat(result).containsExactly(
                new PositionAssignment(C, 1),
                new PositionAssignment(A, 2),
                new PositionAssignment(B, 3));
    }

    @Test
    void move_ShouldReturnNothing_WhenChildIsAlreadyAtThePosition() {
        assertThat(policy.move(List.of(A, B, C), B, 2)).isEmpty();
    }

    @Test
    void move_ShouldAcceptTheLastPosition_WhenNDoesNotCountTheChild() {
        // Three children in total, so N is 2 without the moved child: 1..3 is valid.
        assertThatCode(() -> policy.move(List.of(A, B, C), A, 3)).doesNotThrowAnyException();
    }

    @Test
    void move_ShouldRejectPosition_WhenAboveTotalChildren() {
        assertThatThrownBy(() -> policy.move(List.of(A, B, C), A, 4))
                .isInstanceOfSatisfying(LearningOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(LearningFailureCode.INVALID_POSITION));
    }

    @Test
    void reorder_ShouldReturnAllChildren_WhenEveryChildChangesPlace() {
        List<PositionAssignment> result = policy.reorder(List.of(A, B, C), List.of(C, A, B));

        assertThat(result).containsExactly(
                new PositionAssignment(C, 1),
                new PositionAssignment(A, 2),
                new PositionAssignment(B, 3));
    }

    @Test
    void reorder_ShouldReturnOnlyChangedChildren_WhenTwoChildrenSwap() {
        List<PositionAssignment> result = policy.reorder(List.of(A, B, C), List.of(A, C, B));

        assertThat(result).containsExactly(
                new PositionAssignment(C, 2),
                new PositionAssignment(B, 3));
    }

    @Test
    void reorder_ShouldReturnNothing_WhenOrderIsTheSame() {
        assertThat(policy.reorder(List.of(A, B, C), List.of(A, B, C))).isEmpty();
    }

    @Test
    void reorder_ShouldReturnNothing_WhenParentHasNoChildren() {
        assertThat(policy.reorder(List.of(), List.of())).isEmpty();
    }

    @Test
    void reorder_ShouldRejectRequest_WhenAChildIsMissing() {
        assertThatThrownBy(() -> policy.reorder(List.of(A, B, C), List.of(A, B)))
                .isInstanceOfSatisfying(LearningOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(LearningFailureCode.ORDER_SET_CONFLICT));
    }

    @Test
    void reorder_ShouldRejectRequest_WhenAnUnknownIdIsExtra() {
        assertThatThrownBy(() -> policy.reorder(List.of(A, B), List.of(A, B, NEW)))
                .isInstanceOfSatisfying(LearningOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(LearningFailureCode.ORDER_SET_CONFLICT));
    }

    @Test
    void reorder_ShouldRejectRequest_WhenAnIdIsRepeatedInPlaceOfAnother() {
        assertThatThrownBy(() -> policy.reorder(List.of(A, B, C), List.of(A, A, B)))
                .isInstanceOfSatisfying(LearningOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(LearningFailureCode.ORDER_SET_CONFLICT));
    }

    @Test
    void requireDistinct_ShouldReject_WhenAnIdRepeats() {
        assertThatThrownBy(() -> policy.requireDistinct(List.of(A, B, A)))
                .isInstanceOfSatisfying(LearningOperationException.class,
                        e -> assertThat(e.code()).isEqualTo(LearningFailureCode.VALIDATION_FAILED));
    }

    @Test
    void requireDistinct_ShouldAccept_WhenIdsAreDistinctOrEmpty() {
        assertThatCode(() -> {
            policy.requireDistinct(List.of(A, B, C));
            policy.requireDistinct(List.of());
        }).doesNotThrowAnyException();
    }

    private static UUID id(int suffix) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(suffix));
    }
}
