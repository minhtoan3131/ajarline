package vn.io.ajarline.platform.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import vn.io.ajarline.platform.web.ApiProblemWriter;

public final class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiProblemWriter problems;

    public ApiAuthenticationEntryPoint(ApiProblemWriter problems) {
        this.problems = problems;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        problems.write(
                request,
                response,
                401,
                "AUTHENTICATION_REQUIRED",
                "Authentication required",
                "Authentication required.");
    }
}
