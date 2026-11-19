package vn.io.ajarline.learning.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ArticlePlacement;
import vn.io.ajarline.content.contract.ArticlePlacementPort;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.content.contract.ContentOperationException;
import vn.io.ajarline.content.contract.LearningContextPort;
import vn.io.ajarline.content.contract.PlacementChange;
import vn.io.ajarline.learning.contract.LearningOperationException;
import vn.io.ajarline.learning.infrastructure.persistence.ModuleArticleJpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Lets content put an Article in a Module without knowing how learning keeps the order.
 * Content cannot read learning failures, so the two it can raise are translated here.
 */
@Component
public class ArticlePlacementAdapter implements ArticlePlacementPort, LearningContextPort {
    private final HierarchyPlacement hierarchy;
    private final ModuleArticleJpaRepository memberRepository;

    public ArticlePlacementAdapter(HierarchyPlacement hierarchy,
                                   ModuleArticleJpaRepository memberRepository) {
        this.hierarchy = hierarchy;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockStructure(UUID articleId, UUID targetModuleId) {
        try {
            hierarchy.lockArticleStructure(articleId, targetModuleId);
        } catch (LearningOperationException failure) {
            throw translate(failure);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PlacementChange place(UUID articleId, ArticlePlacement target) {
        try {
            if (target == null) {
                return hierarchy.detachArticle(articleId);
            }
            return hierarchy.placeArticle(articleId, target.moduleId(), target.position());
        } catch (LearningOperationException failure) {
            throw translate(failure);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ArticlePlacement> placementOf(UUID articleId) {
        return memberRepository.findById(articleId)
                .map(member -> new ArticlePlacement(member.getModuleId(), member.getPosition()));
    }

    private ContentOperationException translate(LearningOperationException failure) {
        ContentFailureCode code = switch (failure.code()) {
            case MODULE_REFERENCE_NOT_FOUND -> ContentFailureCode.MODULE_REFERENCE_NOT_FOUND;
            case INVALID_POSITION -> ContentFailureCode.INVALID_POSITION;
            // Any other failure is a bug here. Let it reach the handler as it is.
            default -> throw failure;
        };
        return new ContentOperationException(code, failure.getMessage(), failure);
    }
}
