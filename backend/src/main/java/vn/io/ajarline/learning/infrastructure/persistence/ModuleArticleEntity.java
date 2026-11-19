package vn.io.ajarline.learning.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Membership of one Article in one Module. An Article in no Module has no row.
 */
@Entity
@Table(name = "module_article")
public class ModuleArticleEntity {

    @Id
    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "module_id", nullable = false)
    private UUID moduleId;

    @Column(name = "position", nullable = false)
    private int position;

    /**
     * Required by JPA
     */
    protected ModuleArticleEntity() {

    }

    public ModuleArticleEntity(UUID articleId, UUID moduleId, int position) {
        this.articleId = articleId;
        this.moduleId = moduleId;
        this.position = position;
    }

    /**
     * Moves a sibling inside the same Module.
     */
    public void changePosition(int position) {
        this.position = position;
    }

    /**
     * Moves this Article to a position of a Module. It may be the same Module.
     */
    public void moveTo(UUID moduleId, int position) {
        this.moduleId = moduleId;
        this.position = position;
    }

    public UUID getArticleId() {
        return articleId;
    }

    public UUID getModuleId() {
        return moduleId;
    }

    public int getPosition() {
        return position;
    }
}
