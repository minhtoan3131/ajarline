package vn.io.ajarline.retrieval.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpressionNormalizerTest {

    @Test
    void normalize_ShouldTrimAndCollapseSpaces_WhenTextHasExtraSpaces() {
        assertThat(ExpressionNormalizer.normalize(" Spring  Boot ")).isEqualTo("spring boot");
    }

    @Test
    void normalize_ShouldGiveSameResult_WhenOnlyLetterCaseDiffers() {
        assertThat(ExpressionNormalizer.normalize("SPRING BOOT"))
                .isEqualTo(ExpressionNormalizer.normalize("Spring Boot"));
    }

    @Test
    void normalize_ShouldUseOneSpace_WhenTextHasTabsAndNewLines() {
        assertThat(ExpressionNormalizer.normalize("Spring\tBoot\nCore")).isEqualTo("spring boot core");
    }

    @Test
    void normalize_ShouldUseNfkc_WhenTextHasFullWidthLetters() {
        String fullWidth = "Ｓｐｒｉｎｇ";

        assertThat(ExpressionNormalizer.normalize(fullWidth)).isEqualTo("spring");
    }

    @Test
    void normalize_ShouldTrimNoBreakSpaces_WhenTextStartsAndEndsWithThem() {
        assertThat(ExpressionNormalizer.normalize(" Thread ")).isEqualTo("thread");
    }

    @Test
    void normalize_ShouldKeepAccents_WhenTextIsVietnamese() {
        String withAccent = "má";

        assertThat(ExpressionNormalizer.normalize(withAccent)).isEqualTo(withAccent);
        assertThat(ExpressionNormalizer.normalize(withAccent))
                .isNotEqualTo(ExpressionNormalizer.normalize("ma"));
    }

    @Test
    void normalize_ShouldGiveSameResult_WhenAccentIsWrittenAsOneOrTwoCharacters() {
        String composed = "má";
        String decomposed = "má";

        assertThat(ExpressionNormalizer.normalize(decomposed))
                .isEqualTo(ExpressionNormalizer.normalize(composed));
    }

    @Test
    void normalize_ShouldReturnEmptyText_WhenTextHasOnlySpaces() {
        assertThat(ExpressionNormalizer.normalize("   \t ")).isEmpty();
    }

    @Test
    void normalize_ShouldFail_WhenTextIsNull() {
        assertThatThrownBy(() -> ExpressionNormalizer.normalize(null))
                .isInstanceOf(NullPointerException.class);
    }
}
