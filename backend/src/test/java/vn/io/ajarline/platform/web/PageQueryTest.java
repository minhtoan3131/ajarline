package vn.io.ajarline.platform.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageQueryTest {

    @Test
    void parse_ShouldUseDefaults_WhenNoValueIsGiven() {
        PageQuery query = PageQuery.parse(null, null);

        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(20);
    }

    @ParameterizedTest(name = "[{index}] page={0}, size={1}")
    @CsvSource({"0, 1", "3, 50", "999999999, 20"})
    void parse_ShouldAcceptValues_WhenTheyAreInsideTheLimits(int page, int size) {
        PageQuery query = PageQuery.parse(String.valueOf(page), String.valueOf(size));

        assertThat(query.page()).isEqualTo(page);
        assertThat(query.size()).isEqualTo(size);
    }

    @ParameterizedTest(name = "[{index}] page=\"{0}\"")
    @ValueSource(strings = {"-1", "abc", "1.5", "", "99999999999", "1,2"})
    void parse_ShouldRejectPage_WhenPageIsNotANonNegativeInteger(String page) {
        assertRejected(page, null, "INVALID_PAGE");
    }

    @ParameterizedTest(name = "[{index}] size=\"{0}\"")
    @ValueSource(strings = {"0", "51", "-5", "abc", "", "1,2"})
    void parse_ShouldRejectSize_WhenSizeIsOutsideOneToFifty(String size) {
        assertRejected(null, size, "INVALID_PAGE_SIZE");
    }

    @Test
    void parse_ShouldReportThePage_WhenPageAndSizeAreBothInvalid() {
        assertRejected("abc", "0", "INVALID_PAGE");
    }

    @Test
    void parse_ShouldUseTheCollectionTitle_WhenAValueIsRejected() {
        assertThatThrownBy(() -> PageQuery.parse("-1", null))
                .isInstanceOfSatisfying(ApiQueryException.class, exception -> {
                    assertThat(exception.title()).isEqualTo("Collection request is invalid");
                    assertThat(exception.getMessage()).contains("page");
                });
    }

    private static void assertRejected(String page, String size, String code) {
        assertThatThrownBy(() -> PageQuery.parse(page, size))
                .isInstanceOfSatisfying(ApiQueryException.class,
                        exception -> assertThat(exception.code()).isEqualTo(code));
    }
}
