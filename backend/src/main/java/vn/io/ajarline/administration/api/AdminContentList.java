package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.ContentIndexPage;
import vn.io.ajarline.content.contract.ContentRecordSummary;
import vn.io.ajarline.content.contract.PublicationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One page of the Admin content list. There is no total count. {@code hasMore} tells
 * whether a next page exists.
 */
@Schema(requiredProperties = {"filters", "page", "size", "hasMore", "items"})
public record AdminContentList(
        Filters filters,
        int page,
        int size,
        boolean hasMore,
        List<Item> items
) {

    public AdminContentList {
        items = List.copyOf(items);
    }

    static AdminContentList from(AdminContentQuery query, ContentIndexPage result) {
        return new AdminContentList(
                new Filters(query.typeTokens(), query.statusTokens(), query.q()),
                query.page(),
                query.size(),
                result.hasMore(),
                result.items().stream().map(Item::from).toList());
    }

    /** An empty list means no filter of that kind. {@code q} is null when there is no text filter. */
    @Schema(requiredProperties = {"types", "statuses", "q"})
    public record Filters(
            @Schema(allowableValues = {"series", "module", "article", "news"}) List<String> types,
            @Schema(allowableValues = {"DRAFT", "PUBLISHED", "UNPUBLISHED"}) List<String> statuses,
            @Schema(nullable = true) String q
    ) {

        public Filters {
            types = List.copyOf(types);
            statuses = List.copyOf(statuses);
        }
    }

    @Schema(requiredProperties = {
            "id", "type", "slug", "title", "status", "revision", "publishedAt", "updatedAt"
    })
    public record Item(
            UUID id,
            @Schema(allowableValues = {"series", "module", "article", "news"}) String type,
            String slug,
            String title,
            PublicationStatus status,
            long revision,
            @Schema(nullable = true) Instant publishedAt,
            Instant updatedAt
    ) {

        static Item from(ContentRecordSummary summary) {
            return new Item(
                    summary.id(),
                    AdminContentQuery.wireName(summary.type()),
                    summary.slug(),
                    summary.title(),
                    summary.status(),
                    summary.revision(),
                    summary.publishedAt(),
                    summary.updatedAt());
        }
    }
}
