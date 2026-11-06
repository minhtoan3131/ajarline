package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import vn.io.ajarline.platform.web.OpenApiConfiguration;

/** Maps the authentication HTTP contract to Spring Security session operations. */
@Tag(name = "Authentication", description = "Admin session and CSRF operations.")
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

    @Operation(
            operationId = "getCsrfToken",
            summary = "Get a CSRF token")
    @ApiResponse(
            responseCode = "200",
            description = "CSRF token returned for the current session",
            useReturnTypeSchema = true,
            headers = @Header(
                    name = "Set-Cookie",
                    description = "Session cookie when a session is created"))
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    @Operation(
            operationId = "getSessionState",
            summary = "Get the current Admin session state")
    @GetMapping("/session")
    public SessionResponse session(Authentication authentication) {
        // MVC resolves anonymous requests without an authenticated principal.
        return new SessionResponse(authentication != null && authentication.isAuthenticated());
    }


    @Operation(
            operationId = "loginAdmin",
            summary = "Create an Admin session")
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Token returned by GET /api/auth/v1/csrf.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Admin authenticated and session identifier rotated",
                    useReturnTypeSchema = true,
                    headers = @Header(
                            name = "Set-Cookie",
                            description = "Rotated Admin session cookie")),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid body (INVALID_REQUEST or MALFORMED_JSON)"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credentials are invalid (INVALID_CREDENTIALS)"),
            @ApiResponse(
                    responseCode = "403",
                    description = "CSRF proof is missing or invalid (CSRF_INVALID)"),
            @ApiResponse(
                    responseCode = "415",
                    description = "Content type is not supported (UNSUPPORTED_MEDIA_TYPE)"),
            @ApiResponse(
                    responseCode = "429",
                    description = "Admin login is locked (ADMIN_LOGIN_LOCKED)",
                    headers = @Header(
                            name = "Retry-After",
                            description = "Seconds until another attempt is allowed"))
    })

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SessionResponse login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdminLoginRequest.class)))
            @RequestBody(required = false) JsonNode body,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response) {

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


    @Operation(
            operationId = "logoutAdmin",
            summary = "End the current Admin session")
    @SecurityRequirement(name = OpenApiConfiguration.ADMIN_SESSION)
    @Parameter(
            name = "X-CSRF-TOKEN",
            in = ParameterIn.HEADER,
            required = true,
            description = "Current CSRF token for the authenticated session.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "400",
                    description = "Logout body must be empty (INVALID_REQUEST)"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required (AUTHENTICATION_REQUIRED)"),
            @ApiResponse(
                    responseCode = "403",
                    description = "CSRF proof is missing or invalid (CSRF_INVALID)")
    })
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response,
            @Parameter(hidden = true) Authentication authentication) throws IOException {
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
