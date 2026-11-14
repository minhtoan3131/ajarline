package vn.io.ajarline.topic.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "topic")
public class TopicEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Required by JPA
     */
    protected TopicEntity() {

    }

    private TopicEntity(UUID id, String slug, String name, String description,
                        boolean active, Instant now) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.active = active;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static TopicEntity create(UUID id, String slug, String name, String description,
                                     boolean active, Instant now) {
        return new TopicEntity(id, slug, name, description, active, now);
    }

    public boolean hasSameEditableState(String name, String description, boolean active) {
        return Objects.equals(this.name, name)
                && Objects.equals(this.description, description)
                && this.active == active;
    }

    public void replaceEditableState(String name, String description, boolean active, Instant now) {
        this.name = name;
        this.description = description;
        this.active = active;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
