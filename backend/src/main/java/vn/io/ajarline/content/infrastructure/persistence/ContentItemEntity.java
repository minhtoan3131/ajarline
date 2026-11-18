package vn.io.ajarline.content.infrastructure.persistence;

import jakarta.persistence.*;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicationStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "content_item")
public class ContentItemEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private ContentType type;

    @Column(name = "slug", nullable = false, length = 180)
    private String slug;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PublicationStatus status;

    @Column(name = "revision", nullable = false)
    private long revision;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "unpublished_at")
    private Instant unpublishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Required by JPA
     */
    protected ContentItemEntity() {

    }

    private ContentItemEntity(UUID id, ContentType type, String slug, String title, String summary,
                              PublicationStatus status, long revision, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.type = type;
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.status = status;
        this.revision = revision;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ContentItemEntity draftArticle(
            UUID id, String slug, String title, String summary, Instant now
    ) {
        return draft(id, ContentType.ARTICLE, slug, title, summary, now);
    }

    public static ContentItemEntity draft(
            UUID id, ContentType type, String slug, String title, String summary, Instant now
    ) {
        return new ContentItemEntity(id, type, slug, title, summary, PublicationStatus.DRAFT,
                1, now, now);
    }

    public boolean hasSameEditableContent(
            String title, String slug, String summary
    ) {
        return Objects.equals(this.title, title)
                && Objects.equals(this.slug, slug)
                && Objects.equals(this.summary, summary);
    }

    public void replaceEditTableContent(
            String title, String slug, String summary, Instant now
    ) {
        this.title = title;
        this.slug = slug;
        this.summary = summary;
        this.updatedAt = now;
        this.revision++;
    }

    public boolean hasEverBeenPublished() {
        return this.publishedAt != null;
    }

    public void markPublished(Instant now) {
        Objects.requireNonNull(now, "now");
        if (status == PublicationStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "An effective publish transition requires a non-published state.");
        }
        status = PublicationStatus.PUBLISHED;
        if (publishedAt == null) {
            publishedAt = now;
        }
        updatedAt = now;
        revision++;
    }

    public void markUnpublished(Instant now) {
        Objects.requireNonNull(now, "now");
        if (status != PublicationStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "An effective unpublish transition requires PUBLISHED state.");
        }
        status = PublicationStatus.UNPUBLISHED;
        unpublishedAt = now;
        updatedAt = now;
        revision++;
    }


    public UUID getId() {
        return id;
    }

    public ContentType getType() {
        return type;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public long getRevision() {
        return revision;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getUnpublishedAt() {
        return unpublishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
