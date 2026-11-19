package vn.io.ajarline.topic.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.platform.web.ApiQueryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TopicContentQueryTest {

    @Test
    void parse_ShouldUseDefaults_WhenNoValueIsGiven() {
        TopicContentQuery query = TopicContentQuery.parse(null, null, null);

        assertThat(query.typeTokens()).isEmpty();
        assertThat(query.contentTypes()).containsExactlyInAnyOrder(ContentType.values());
        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(20);
    }

    @Test
    void parse_ShouldTrimTokensAndKeepTheirOrder_WhenTypesAreGiven() {
        TopicContentQuery query = TopicContentQuery.parse(" news , article", null, null);

        assertThat(query.typeTokens()).containsExactly("news", "article");
        assertThat(query.contentTypes()).contains(ContentType.ARTICLE);
    }

    @Test
    void parse_ShouldNotSelectArticles_WhenOnlyAnotherTypeIsGiven() {
        TopicContentQuery query = TopicContentQuery.parse("series", null, null);

        assertThat(query.typeTokens()).containsExactly("series");
        assertThat(query.contentTypes()).doesNotContain(ContentType.ARTICLE);
    }

    @Test
    void parse_ShouldKeepPageAndSize_WhenTheyAreGiven() {
        TopicContentQuery query = TopicContentQuery.parse(null, "2", "50");

        assertThat(query.page()).isEqualTo(2);
        assertThat(query.size()).isEqualTo(50);
    }

    @ParameterizedTest(name = "[{index}] types=\"{0}\"")
    @ValueSource(strings = {"", " ", "article,", ",article", "Article", "blog", "article,article", "article, article"})
    void parse_ShouldRejectTypes_WhenTokenIsEmptyUnknownOrRepeated(String types) {
        assertThatThrownBy(() -> TopicContentQuery.parse(types, null, null))
                .isInstanceOfSatisfying(ApiQueryException.class,
                        exception -> assertThat(exception.code()).isEqualTo("INVALID_RESULT_TYPE"));
    }
}
