package vn.io.ajarline.platform.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.filter.OncePerRequestFilter;

public final class ApiRequestFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_ATTRIBUTE =
            ApiRequestFilter.class.getName() + ".requestId";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        request.setAttribute(
                REQUEST_ID_ATTRIBUTE,
                UUID.randomUUID().toString());

        String path = request.getServletPath();
        if (path.startsWith("/api/auth/")
                || path.startsWith("/api/admin/")) {
            response.setHeader("Cache-Control", "no-store");
        }
        filterChain.doFilter(request, response);
    }
}
