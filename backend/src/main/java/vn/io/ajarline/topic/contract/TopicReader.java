package vn.io.ajarline.topic.contract;

import java.util.List;

public interface TopicReader {

    /**
     * Returns every Topic, active or not, ordered by name and then by id.
     */
    List<TopicView> list();
}
