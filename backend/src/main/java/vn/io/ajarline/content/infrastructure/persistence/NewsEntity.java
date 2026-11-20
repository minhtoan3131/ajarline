package vn.io.ajarline.content.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * News-only data. The common record is the content_item row with the same id.
 * It has no version field: only content raises the revision.
 */
@Entity
@Table(name = "news")
public class NewsEntity {

    @Id
    @Column(name = "content_id", nullable = false, updatable = false)
    private UUID contentId;

    @Column(name = "body_markdown", columnDefinition = "text")
    private String bodyMarkdown;

    @Column(name = "source_name", length = 300)
    private String sourceName;

    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    @Column(name = "source_published_at")
    private Instant sourcePublishedAt;

    /**
     * Required by JPA
     */
    protected NewsEntity() {

    }

    public NewsEntity(UUID contentId, String bodyMarkdown,
                      String sourceName, String sourceUrl, Instant sourcePublishedAt) {
        this.contentId = contentId;
        this.bodyMarkdown = bodyMarkdown;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.sourcePublishedAt = sourcePublishedAt;
    }

    public boolean hasSameBody(String bodyMarkdown) {
        return Objects.equals(this.bodyMarkdown, bodyMarkdown);
    }

    /**
     * Compares the time as an instant, so the offset the client used does not matter.
     */
    public boolean hasSameSource(String sourceName, String sourceUrl, Instant sourcePublishedAt) {
        return Objects.equals(this.sourceName, sourceName)
                && Objects.equals(this.sourceUrl, sourceUrl)
                && Objects.equals(this.sourcePublishedAt, sourcePublishedAt);
    }

    public void replaceBody(String bodyMarkdown) {
        this.bodyMarkdown = bodyMarkdown;
    }

    public void replaceSource(String sourceName, String sourceUrl, Instant sourcePublishedAt) {
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.sourcePublishedAt = sourcePublishedAt;
    }

    public UUID getContentId() {
        return contentId;
    }

    public String getBodyMarkdown() {
        return bodyMarkdown;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Instant getSourcePublishedAt() {
        return sourcePublishedAt;
    }
}
