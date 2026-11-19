package vn.io.ajarline.content.contract;

import java.util.UUID;

/**
 * Puts an Article in a Module, or takes it out. Content defines this port and learning
 * implements it, so content never imports learning. Every method needs the caller's transaction.
 *
 * <p>Call {@link #lockStructure} before the Article row is locked, and {@link #place} after.
 * Then every writer locks the parent before the child.</p>
 */
public interface ArticlePlacementPort {

    /**
     * Locks the Module the Article is in now and the target Module, in ascending id order.
     *
     * @param targetModuleId null when the Article will be standalone
     * @throws ContentOperationException with {@link ContentFailureCode#MODULE_REFERENCE_NOT_FOUND}
     *         when the target is not a Module
     */
    void lockStructure(UUID articleId, UUID targetModuleId);

    /**
     * Moves the Article to the target and closes the gap it leaves. The Modules must be locked.
     * It writes positions but raises no revision. The result lists the ids that need one.
     *
     * @param target null to make the Article standalone
     * @throws ContentOperationException with {@link ContentFailureCode#INVALID_POSITION}
     *         when the position is outside {@code 1..N+1}; nothing is written then
     */
    PlacementChange place(UUID articleId, ArticlePlacement target);
}
