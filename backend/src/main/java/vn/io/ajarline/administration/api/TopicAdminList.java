package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import vn.io.ajarline.topic.contract.TopicView;

import java.util.List;

/**
 * Represents the Topic list returned by the Admin API. The list is not paged.
 */
@Schema(requiredProperties = {"topics"})
public record TopicAdminList(List<TopicAdminItem> topics) {

    static TopicAdminList from(List<TopicView> views) {
        return new TopicAdminList(views.stream().map(TopicAdminItem::from).toList());
    }
}
