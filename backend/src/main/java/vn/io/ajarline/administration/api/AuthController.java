package vn.io.ajarline.administration.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Maps the authentication HTTP contract to Spring Security session operations. */
@RestController
@RequestMapping("/api/auth/v1")
public class AuthController {

    private final AuthenticationManager manager;
    private final SessionAuthenticationStrategy sessions;
    private final SecurityContextRepository contexts;
    private final SecurityContextLogoutHandler securityLogout = new SecurityContextLogoutHandler();
    private final CsrfLogoutHandler csrfLogout;

    public AuthController(
            AuthenticationManager manager, SessionAuthenticationStrategy sessions,
            SecurityContextRepository contexts, HttpSessionCsrfTokenRepository csrfRepository) {
        this.manager = manager;
        this.sessions = sessions;
        this.contexts = contexts;
        this.csrfLogout = new CsrfLogoutHandler(csrfRepository);
        securityLogout.setSecurityContextRepository(contexts);
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    @GetMapping("/session")
    public SessionResponse session(Authentication authentication) {
        // MVC resolves anonymous requests without an authenticated principal.
        return new SessionResponse(authentication != null && authentication.isAuthenticated());
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SessionResponse login(
            @RequestBody(required = false) JsonNode body,
            HttpServletRequest request, HttpServletResponse response) {
        AdminLoginRequest credentials = AdminLoginRequest.parse(body);
        Authentication authentication = manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        credentials.username(), credentials.password()));

        sessions.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return new SessionResponse(true);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        if (request.getInputStream().read() != -1) {
            throw new InvalidAuthRequestException();
        }
        csrfLogout.logout(request, response, authentication);
        securityLogout.logout(request, response, authentication);
    }

    public record CsrfResponse(String token, String headerName) {
    }

    public record SessionResponse(boolean authenticated) {
    }

    /** Signals an invalid request shape without keeping the submitted credentials. */
    public static final class InvalidAuthRequestException extends RuntimeException {
    }
}
