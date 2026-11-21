package vn.io.ajarline.retrieval.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.retrieval.infrastructure.persistence.VocabularyStateJpaRepository;

import java.time.Instant;

/**
 * Raises the vocabulary version by one.
 *
 * <p>The caller must already be in a transaction. The version then commits or
 * rolls back together with the Concept change. The statement locks the version row
 * until the transaction ends, so overlapping changes wait for each other.</p>
 */
@Component
public class VocabularyVersion {

    private final VocabularyStateJpaRepository states;

    public VocabularyVersion(VocabularyStateJpaRepository states) {
        this.states = states;
    }

    /**
     * @throws IllegalStateException when the version row is missing
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void advance(Instant now) {
        int changedRows = states.advance(now);
        if (changedRows != 1) {
            throw new IllegalStateException(
                    "The vocabulary version row must change exactly once, but changed " + changedRows + ".");
        }
    }
}
