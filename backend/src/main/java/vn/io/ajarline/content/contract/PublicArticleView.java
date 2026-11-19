package vn.io.ajarline.content.contract;

import java.util.List;
import java.util.Objects;

/**
 * Holds a public Article together with the Topics shown with it and what readers can see
 * around it in the learning hierarchy.
 */
public record PublicArticleView(
        ArticleView article,
        List<TopicLabel> topics,
        PublicArticleContext learning
) {

    public PublicArticleView {
        topics = List.copyOf(topics);
        Objects.requireNonNull(learning, "learning");
    }
}
