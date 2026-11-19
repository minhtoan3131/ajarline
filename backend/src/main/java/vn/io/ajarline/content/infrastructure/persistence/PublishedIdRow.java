package vn.io.ajarline.content.infrastructure.persistence;

import vn.io.ajarline.content.contract.ContentType;

import java.util.UUID;

/**
 * Receives one row of a query that needs only the id and the type of an item.
 * Spring Data fills it from the column aliases.
 */
public interface PublishedIdRow {

    UUID getId();

    ContentType getContentType();
}
