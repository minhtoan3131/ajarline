package vn.io.ajarline.topic.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.content.contract.TopicLabel;

import java.util.List;

/**
 * Represents the public Topic list. It is not paged and carries no counts.
 */
@Schema(requiredProperties = {"topics"})
public record TopicDiscoveryResponse(List<TopicRef> topics) {

    public TopicDiscoveryResponse {
        topics = List.copyOf(topics);
    }

    static TopicDiscoveryResponse from(List<TopicLabel> labels) {
        return new TopicDiscoveryResponse(labels.stream().map(TopicRef::from).toList());
    }
}
