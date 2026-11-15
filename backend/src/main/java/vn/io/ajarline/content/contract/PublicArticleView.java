package vn.io.ajarline.content.contract;

import java.util.List;

/**
 * Holds a public Article together with the Topics shown with it.
 */
public record PublicArticleView(
        ArticleView article,
        List<TopicLabel> topics
) {

    public PublicArticleView {
        topics = List.copyOf(topics);
    }
}
