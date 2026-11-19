package vn.io.ajarline.learning.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Series-only data. The common record is the content_item row with the same id.
 */
@Entity
@Table(name = "series")
public class SeriesEntity {

    @Id
    @Column(name = "content_id", nullable = false, updatable = false)
    private UUID contentId;

    @Column(name = "overview_markdown", columnDefinition = "text")
    private String overviewMarkdown;

    /**
     * Required by JPA
     */
    protected SeriesEntity() {

    }

    public SeriesEntity(UUID contentId, String overviewMarkdown) {
        this.contentId = contentId;
        this.overviewMarkdown = overviewMarkdown;
    }

    public boolean hasSameOverview(String overviewMarkdown) {
        return Objects.equals(this.overviewMarkdown, overviewMarkdown);
    }

    public void replaceOverview(String overviewMarkdown) {
        this.overviewMarkdown = overviewMarkdown;
    }

    public UUID getContentId() {
        return contentId;
    }

    public String getOverviewMarkdown() {
        return overviewMarkdown;
    }
}
