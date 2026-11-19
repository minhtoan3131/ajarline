package vn.io.ajarline.content.application;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Decides whether a source URL is safe to keep and to show as a link.
 *
 * <p>It must be at least as strict as the URL policy of the Markdown renderer. A URL that
 * passes here must never be dropped there.</p>
 */
@Component
public class SourceUrlPolicy {

    // "http" or "https", then "://", then an optional user part ending with "@", then a host.
    // A host cannot start with ":", "/", "?", "#" or "@", so "http://:80" and "http:///x" fail.
    private static final Pattern HTTP_URL_START =
            Pattern.compile("(?i:https?)://(?:[^/?#@]*@)?[^/?#@:]");

    /**
     * Returns true for an absolute http or https URL with a host and no unsafe character.
     * The caller trims the value first. A space inside the URL makes it invalid.
     */
    public boolean isValid(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return !hasUnsafeCharacter(url) && HTTP_URL_START.matcher(url).lookingAt();
    }

    /**
     * Browsers drop tabs and line breaks inside a URL, so "java\tscript:" can still run.
     * A backslash can turn "/\host" into another host. These characters are rejected,
     * not repaired.
     */
    private static boolean hasUnsafeCharacter(String url) {
        return url.chars().anyMatch(c -> c <= ' ' || c == 0x7f || c == '\\');
    }
}
