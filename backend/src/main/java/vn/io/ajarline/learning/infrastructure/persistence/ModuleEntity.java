package vn.io.ajarline.learning.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Module-only data, with its Series and position. The Series is referenced by id only.
 */
@Entity
@Table(name = "module")
public class ModuleEntity {

    @Id
    @Column(name = "content_id", nullable = false, updatable = false)
    private UUID contentId;

    @Column(name = "series_id", nullable = false)
    private UUID seriesId;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "overview_markdown", columnDefinition = "text")
    private String overviewMarkdown;

    /**
     * Required by JPA
     */
    protected ModuleEntity() {

    }

    public ModuleEntity(UUID contentId, UUID seriesId, int position, String overviewMarkdown) {
        this.contentId = contentId;
        this.seriesId = seriesId;
        this.position = position;
        this.overviewMarkdown = overviewMarkdown;
    }

    public boolean hasSameOverview(String overviewMarkdown) {
        return Objects.equals(this.overviewMarkdown, overviewMarkdown);
    }

    public void replaceOverview(String overviewMarkdown) {
        this.overviewMarkdown = overviewMarkdown;
    }

    /**
     * Moves a sibling inside the same Series.
     */
    public void changePosition(int position) {
        this.position = position;
    }

    /**
     * Moves this Module to a position of a Series. It may be the same Series.
     */
    public void moveTo(UUID seriesId, int position) {
        this.seriesId = seriesId;
        this.position = position;
    }

    public UUID getContentId() {
        return contentId;
    }

    public UUID getSeriesId() {
        return seriesId;
    }

    public int getPosition() {
        return position;
    }

    public String getOverviewMarkdown() {
        return overviewMarkdown;
    }
}
