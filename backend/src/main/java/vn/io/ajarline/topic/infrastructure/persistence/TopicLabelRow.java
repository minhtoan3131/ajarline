package vn.io.ajarline.topic.infrastructure.persistence;

import vn.io.ajarline.content.contract.TopicLabel;

import java.util.UUID;

/**
 * Receives one row of a Topic label query: the content item and one Topic
 * assigned to it. Spring Data fills it from the column aliases.
 */
public interface TopicLabelRow {

    UUID getContentId();

    UUID getId();

    String getSlug();

    String getName();

    default TopicLabel toLabel() {
        return new TopicLabel(getId(), getSlug(), getName());
    }
}
