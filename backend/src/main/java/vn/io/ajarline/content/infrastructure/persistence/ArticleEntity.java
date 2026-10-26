package vn.io.ajarline.content.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "article")
public class ArticleEntity {

    @Id
    @Column(name = "content_id", nullable = false, updatable = false)
    private UUID contentId;

    @Column(name = "body_markdown", columnDefinition = "text")
    private String bodyMarkdown;

    /**
     * Required by JPA
     */
    protected ArticleEntity() {

    }
}
