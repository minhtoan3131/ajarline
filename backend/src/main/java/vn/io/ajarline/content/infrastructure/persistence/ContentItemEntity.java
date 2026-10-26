package vn.io.ajarline.content.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_item")
public class ContentItemEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "type", nullable = false, length = 16)
    private String type;

    @Column(name = "slug", nullable = false, length = 180)
    private String slug;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "revision", nullable = false)
    private long revision;

    @Column(name = "published_at")
    private Instant published_at;

    @Column(name = "unpublished_at")
    private Instant unpublished_at;

    @Column(name = "created_at", nullable = false)
    private Instant created_at;

    @Column(name = "updated_at", nullable = false)
    private Instant updated_at;

    /**
     * Required by JPA
     */
    protected ContentItemEntity() {

    }
}
