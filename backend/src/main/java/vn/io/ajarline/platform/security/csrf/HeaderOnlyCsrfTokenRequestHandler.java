package vn.io.ajarline.platform.security.csrf;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

import java.util.function.Supplier;

/**
 * Only accepts CSRF tokens from the request header.
 *
 * <p>Spring Security also accepts tokens from request parameters by default.
 * This class blocks that behavior. All other handling uses Spring Security's
 * default implementation.</p>
 */
public final class HeaderOnlyCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
    private final XorCsrfTokenRequestAttributeHandler delegate = new XorCsrfTokenRequestAttributeHandler();
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        delegate.handle(request, response, csrfToken);
    }

    @Override
    public @Nullable String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        // Reject tokens sent only through request parameters.
        if (request.getHeader(csrfToken.getHeaderName()) == null) {
            return null;
        }
        return delegate.resolveCsrfTokenValue(request, csrfToken);
    }
}
