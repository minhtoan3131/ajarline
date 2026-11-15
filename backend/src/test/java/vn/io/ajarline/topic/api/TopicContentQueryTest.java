package vn.io.ajarline.topic.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.io.ajarline.content.contract.ContentType;

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
    void parse_ShouldAcceptSizeLimits_WhenSizeIsOneOrFifty() {
        assertThat(TopicContentQuery.parse(null, null, "1").size()).isEqualTo(1);
        assertThat(TopicContentQuery.parse(null, null, "50").size()).isEqualTo(50);
    }

    @ParameterizedTest(name = "[{index}] types=\"{0}\"")
    @ValueSource(strings = {"", " ", "article,", ",article", "Article", "blog", "article,article", "article, article"})
    void parse_ShouldRejectTypes_WhenTokenIsEmptyUnknownOrRepeated(String types) {
        assertRejected(types, null, null, "INVALID_RESULT_TYPE");
    }

    @ParameterizedTest(name = "[{index}] page=\"{0}\"")
    @ValueSource(strings = {"-1", "abc", "1.5", "", "99999999999"})
    void parse_ShouldRejectPage_WhenPageIsNotANonNegativeInteger(String page) {
        assertRejected(null, page, null, "INVALID_PAGE");
    }

    @ParameterizedTest(name = "[{index}] size=\"{0}\"")
    @ValueSource(strings = {"0", "51", "-5", "abc", ""})
    void parse_ShouldRejectSize_WhenSizeIsOutsideOneToFifty(String size) {
        assertRejected(null, null, size, "INVALID_PAGE_SIZE");
    }

    private static void assertRejected(String types, String page, String size, String code) {
        assertThatThrownBy(() -> TopicContentQuery.parse(types, page, size))
                .isInstanceOfSatisfying(PublicTopicQueryException.class,
                        exception -> assertThat(exception.code()).isEqualTo(code));
    }
}
