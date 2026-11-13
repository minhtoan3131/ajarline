package vn.io.ajarline.content.contract;

import java.util.UUID;

public interface ArticleEditor {
    ArticleView replace(
            UUID id,
            long expectedRevision,
            ArticleContentInput input
    );
}
