package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vn.io.ajarline.content.contract.ContentIndexFilter;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.platform.web.ApiQueryException;

class AdminContentQueryTest {

    @Test
    void parse_ShouldUseDefaults_WhenNoValueIsGiven() {
        AdminContentQuery query = AdminContentQuery.parse(null, null, null, null, null);

        assertThat(query.typeTokens()).isEmpty();
        assertThat(query.statusTokens()).isEmpty();
        assertThat(query.q()).isNull();
        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(20);
    }

    @Test
    void parse_ShouldTrimTokensAndText_WhenValuesHaveSpaces() {
        AdminContentQuery query = AdminContentQuery.parse(
                "series, module", " DRAFT , PUBLISHED", "  alpha  ", "2", "100");

        assertThat(query.typeTokens()).containsExactly("series", "module");
        assertThat(query.statusTokens()).containsExactly("DRAFT", "PUBLISHED");
        assertThat(query.q()).isEqualTo("alpha");
        assertThat(query.page()).isEqualTo(2);
        assertThat(query.size()).isEqualTo(100);
    }

    @ParameterizedTest(name = "[{index}] types=\"{0}\"")
    @ValueSource(strings = {"book", "Series", "SERIES", "series,series", "series,", ",series", ""})
    void parse_ShouldRejectTypes_WhenATokenIsUnknownRepeatedOrEmpty(String types) {
        assertRejected(types, null, null, null, null, "INVALID_RESULT_TYPE");
    }

    @ParameterizedTest(name = "[{index}] statuses=\"{0}\"")
    @ValueSource(strings = {"ARCHIVED", "draft", "DRAFT,DRAFT", "DRAFT,", ""})
    void parse_ShouldRejectStatuses_WhenATokenIsUnknownRepeatedOrEmpty(String statuses) {
        assertRejected(null, statuses, null, null, null, "INVALID_REQUEST");
    }

    @ParameterizedTest(name = "[{index}] q has {0} characters")
    @ValueSource(ints = {0, 301})
    void parse_ShouldRejectText_WhenItIsEmptyOrTooLong(int length) {
        assertRejected(null, null, "a".repeat(length), null, null, "INVALID_REQUEST");
    }

    @Test
    void parse_ShouldRejectText_WhenItIsOnlySpaces() {
        assertRejected(null, null, "   ", null, null, "INVALID_REQUEST");
    }

    @Test
    void parse_ShouldAcceptText_WhenItHasTheLongestLength() {
        AdminContentQuery query = AdminContentQuery.parse(null, null, "a".repeat(300), null, null);

        assertThat(query.q()).hasSize(300);
    }

    @Test
    void parse_ShouldRejectSize_WhenItIsAboveOneHundred() {
        assertRejected(null, null, null, null, "101", "INVALID_PAGE_SIZE");
    }

    @Test
    void parse_ShouldRejectPage_WhenItIsNegative() {
        assertRejected(null, null, null, "-1", null, "INVALID_PAGE");
    }

    @Test
    void toFilter_ShouldSelectEveryTypeAndStatus_WhenNoTokenIsGiven() {
        ContentIndexFilter filter = AdminContentQuery.parse(null, null, null, null, null).toFilter();

        assertThat(filter.types()).containsExactlyInAnyOrder(ContentType.values());
        assertThat(filter.statuses()).containsExactlyInAnyOrder(PublicationStatus.values());
        assertThat(filter.q()).isNull();
    }

    @Test
    void toFilter_ShouldSelectOnlyTheNamedValues_WhenTokensAreGiven() {
        ContentIndexFilter filter = AdminContentQuery
                .parse("series", "DRAFT,UNPUBLISHED", "alpha", null, null).toFilter();

        assertThat(filter.types()).isEqualTo(Set.of(ContentType.SERIES));
        assertThat(filter.statuses())
                .containsExactlyInAnyOrder(PublicationStatus.DRAFT, PublicationStatus.UNPUBLISHED);
        assertThat(filter.q()).isEqualTo("alpha");
    }

    @Test
    void toFilter_ShouldSelectNews_WhenOnlyNewsIsNamed() {
        ContentIndexFilter filter = AdminContentQuery.parse("news", null, null, null, null).toFilter();

        assertThat(filter.types()).containsExactly(ContentType.NEWS);
    }

    private static void assertRejected(
            String types, String statuses, String q, String page, String size, String code) {
        assertThatThrownBy(() -> AdminContentQuery.parse(types, statuses, q, page, size))
                .isInstanceOfSatisfying(ApiQueryException.class,
                        exception -> assertThat(exception.code()).isEqualTo(code));
    }
}
