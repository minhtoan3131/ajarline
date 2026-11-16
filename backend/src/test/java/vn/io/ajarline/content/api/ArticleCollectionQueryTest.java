package vn.io.ajarline.content.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.io.ajarline.platform.web.ApiQueryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleCollectionQueryTest {

    @Test
    void parse_ShouldUseDefaults_WhenOnlyStandaloneIsTrue() {
        ArticleCollectionQuery query = ArticleCollectionQuery.parse("true", null, null);

        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(20);
    }

    @Test
    void parse_ShouldKeepPageAndSize_WhenTheyAreGiven() {
        ArticleCollectionQuery query = ArticleCollectionQuery.parse("true", "2", "50");

        assertThat(query.page()).isEqualTo(2);
        assertThat(query.size()).isEqualTo(50);
    }

    @ParameterizedTest(name = "[{index}] standalone=\"{0}\"")
    @NullSource
    @ValueSource(strings = {"", "false", "TRUE", "True", " true", "1", "abc", "true,true"})
    void parse_ShouldRejectStandalone_WhenItIsNotExactlyTrue(String standalone) {
        assertRejected(standalone, null, null, "INVALID_ARTICLE_COLLECTION_MODE");
    }

    @Test
    void parse_ShouldReportStandalone_WhenPageAndSizeAreAlsoInvalid() {
        assertRejected("false", "-1", "0", "INVALID_ARTICLE_COLLECTION_MODE");
    }

    @Test
    void parse_ShouldReportPage_WhenStandaloneIsValidAndPageAndSizeAreInvalid() {
        assertRejected("true", "abc", "0", "INVALID_PAGE");
    }

    @Test
    void parse_ShouldReportSize_WhenOnlySizeIsInvalid() {
        assertRejected("true", "1", "51", "INVALID_PAGE_SIZE");
    }

    @Test
    void parse_ShouldUseTheArticleCollectionTitle_WhenStandaloneIsRejected() {
        assertThatThrownBy(() -> ArticleCollectionQuery.parse(null, null, null))
                .isInstanceOfSatisfying(ApiQueryException.class, exception -> {
                    assertThat(exception.title()).isEqualTo("Article collection request is invalid");
                    assertThat(exception.getMessage()).contains("standalone");
                });
    }

    private static void assertRejected(String standalone, String page, String size, String code) {
        assertThatThrownBy(() -> ArticleCollectionQuery.parse(standalone, page, size))
                .isInstanceOfSatisfying(ApiQueryException.class,
                        exception -> assertThat(exception.code()).isEqualTo(code));
    }
}
