package vn.io.ajarline.topic.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.PublicContentContext;
import vn.io.ajarline.content.contract.PublicContentSummary;
import vn.io.ajarline.topic.application.PublicTopicContent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents one page of the public content directly assigned to a Topic.
 * There is no total count; {@code hasMore} tells whether a next page exists.
 */
@Schema(requiredProperties = {"filters", "page", "size", "hasMore", "items"})
public record TopicContentResponse(
        TopicContentFilters filters,
        int page,
        int size,
        boolean hasMore,
        List<TopicContentItem> items
) {

    public TopicContentResponse {
        items = List.copyOf(items);
    }

    static TopicContentResponse from(TopicContentQuery query, PublicTopicContent content) {
        return new TopicContentResponse(
                new TopicContentFilters(query.typeTokens()),
                query.page(),
                query.size(),
                content.hasMore(),
                content.items().stream().map(TopicContentItem::from).toList());
    }

    /** An empty list means no type filter. */
    @Schema(requiredProperties = {"types"})
    public record TopicContentFilters(
            @Schema(allowableValues = {"series", "module", "article", "news"}) List<String> types
    ) {
        public TopicContentFilters {
            types = List.copyOf(types);
        }
    }

    @Schema(requiredProperties = {
            "id", "type", "slug", "title", "summary",
            "topics", "publishedAt", "updatedAt", "context"
    })
    public record TopicContentItem(
            UUID id,
            @Schema(allowableValues = {"series", "module", "article", "news"}) String type,
            String slug,
            String title,
            String summary,
            List<TopicRef> topics,
            Instant publishedAt,
            Instant updatedAt,
            TopicContentContext context
    ) {
        public TopicContentItem {
            topics = List.copyOf(topics);
        }

        static TopicContentItem from(PublicTopicContent.Entry entry) {
            PublicContentSummary content = entry.content();
            return new TopicContentItem(
                    content.id(),
                    TopicContentQuery.wireName(content.type()),
                    content.slug(),
                    content.title(),
                    content.summary(),
                    entry.topics().stream().map(TopicRef::from).toList(),
                    content.publishedAt(),
                    content.updatedAt(),
                    TopicContentContext.from(content.context()));
        }
    }

    /**
     * Holds the public parents of an item. A parent that is not public is
     * always null.
     */
    @Schema(requiredProperties = {"series", "module"})
    public record TopicContentContext(
            @Schema(nullable = true) SeriesContextRef series,
            @Schema(nullable = true) ModuleContextRef module
    ) {

        static TopicContentContext from(PublicContentContext context) {
            return new TopicContentContext(
                    context.series() == null ? null : new SeriesContextRef(
                            context.series().id(),
                            context.series().slug(),
                            context.series().title()),
                    context.module() == null ? null : new ModuleContextRef(
                            context.module().id(),
                            context.module().slug(),
                            context.module().title(),
                            context.module().position()));
        }
    }

    @Schema(requiredProperties = {"id", "slug", "title"})
    public record SeriesContextRef(
            UUID id,
            String slug,
            String title
    ) {
    }

    @Schema(requiredProperties = {"id", "slug", "title", "position"})
    public record ModuleContextRef(
            UUID id,
            String slug,
            String title,
            int position
    ) {
    }
}
