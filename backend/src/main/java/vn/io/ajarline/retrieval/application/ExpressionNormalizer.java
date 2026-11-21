package vn.io.ajarline.retrieval.application;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Computes the normalized form of an expression.
 *
 * <p>The steps are Unicode NFKC, lowercase, one space for each run of white space,
 * then trim. Accents stay, so "má" and "ma" are different. Search must use this same
 * function for query text.</p>
 */
public final class ExpressionNormalizer {

    private static final Pattern WHITE_SPACE =
            Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private ExpressionNormalizer() {
    }

    public static String normalize(String expression) {
        String composed = Normalizer.normalize(expression, Normalizer.Form.NFKC);
        String lowercase = composed.toLowerCase(Locale.ROOT);
        // Trim last. NFKC turns a no-break space into a normal space.
        return WHITE_SPACE.matcher(lowercase).replaceAll(" ").strip();
    }
}
