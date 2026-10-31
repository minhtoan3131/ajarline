package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import vn.io.ajarline.support.AdminAuthenticationTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminSecurityPolicyIT extends AbstractAdminAuthenticationIT {

    @Test
    void csrf_ShouldIssueHostOnlySessionCookie_WhenClientBootstraps() throws Exception {
        Browser browser = new Browser();

        HttpResponse<String> response = browser.get(AUTH + "/csrf");

        assertThat(response.statusCode()).isEqualTo(200);
        assertNoStore(response);
        String header = response.headers().allValues("Set-Cookie").stream()
                .filter(value -> value.startsWith("AJARLINE_SESSION="))
                .findFirst()
                .orElseThrow();
        // Assert attributes without printing the sensitive cookie value.
        String attributes = header.substring(header.indexOf(';')).toLowerCase(Locale.ROOT);
        assertThat(attributes)
                .contains("; path=/api", "; secure", "; httponly", "samesite=lax");
        assertThat(attributes).doesNotContain("domain=", "max-age=", "expires=");
        assertThat(servletContext.getSessionTimeout()).isEqualTo(120);
    }

    @Test
    void login_ShouldRejectRequest_WhenCsrfProofIsMissingOrWrong() throws Exception {
        Browser browser = new Browser();
        csrf(browser);

        HttpResponse<String> missing = login(browser, null,
                AdminAuthenticationTestData.username(),
                AdminAuthenticationTestData.password());
        HttpResponse<String> wrong = login(browser, "invalid",
                AdminAuthenticationTestData.username(),
                AdminAuthenticationTestData.password());

        assertProblem(missing, 403, "CSRF_INVALID");
        assertProblem(wrong, 403, "CSRF_INVALID");
        assertJson(browser.get(AUTH + "/session"), 200, "{\"authenticated\":false}");
    }

    @Test
    void mutate_ShouldRejectRequest_WhenAuthenticatedClientLacksCsrfProof() throws Exception {
        Browser browser = authenticatedBrowser();

        HttpResponse<String> missing = browser.post(ADMIN_PROBE, null, null);
        HttpResponse<String> wrong = browser.post(ADMIN_PROBE, null, "invalid");
        HttpResponse<String> logout = browser.post(AUTH + "/logout", null, null);
        HttpResponse<String> wrongLogout = browser.post(
                AUTH + "/logout", null, "invalid");

        assertProblem(missing, 403, "CSRF_INVALID");
        assertProblem(wrong, 403, "CSRF_INVALID");
        assertProblem(logout, 403, "CSRF_INVALID");
        assertProblem(wrongLogout, 403, "CSRF_INVALID");
        assertThat(mutationCount()).isZero();
        assertJson(browser.get(AUTH + "/session"), 200, "{\"authenticated\":true}");
    }

    @Test
    void login_ShouldLockAcrossSessions_WhenFifthCredentialAttemptFails() throws Exception {
        Browser first = new Browser();
        String token = csrf(first);
        for (int attempt = 0; attempt < 4; attempt++) {
            assertProblem(login(first, token,
                            AdminAuthenticationTestData.username(), "invalid"),
                    401, "INVALID_CREDENTIALS");
        }

        HttpResponse<String> fifth = login(first, token,
                AdminAuthenticationTestData.username(), "invalid");

        assertProblem(fifth, 429, "ADMIN_LOGIN_LOCKED");
        assertThat(fifth.headers().firstValue("Retry-After").orElseThrow())
                .isEqualTo("900");

        Browser another = new Browser();
        String anotherToken = csrf(another);
        assertProblem(login(another, anotherToken,
                        AdminAuthenticationTestData.username(),
                        AdminAuthenticationTestData.password()),
                429, "ADMIN_LOGIN_LOCKED");
        assertJson(another.get(AUTH + "/session"),
                200, "{\"authenticated\":false}");

        LOGIN_CLOCK.set(LOGIN_CLOCK.instant().plusSeconds(899).plusMillis(1));
        HttpResponse<String> almostExpired = login(another, anotherToken,
                AdminAuthenticationTestData.username(),
                AdminAuthenticationTestData.password());
        assertProblem(almostExpired, 429, "ADMIN_LOGIN_LOCKED");
        assertThat(almostExpired.headers().firstValue("Retry-After").orElseThrow())
                .isEqualTo("1");

        LOGIN_CLOCK.set(LOGIN_CLOCK.instant().plusMillis(999));
        assertJson(login(another, anotherToken,
                        AdminAuthenticationTestData.username(),
                        AdminAuthenticationTestData.password()),
                200, "{\"authenticated\":true}");
    }

    @Test
    void csrf_ShouldNotGrantCorsAccess_WhenOriginIsExternal() throws Exception {
        HttpClient client = httpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + AUTH + "/csrf"))
                .timeout(Duration.ofSeconds(10))
                .header("Origin", "https://external.example")
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
                .isEmpty();
        assertThat(response.headers().firstValue("Access-Control-Allow-Credentials"))
                .isEmpty();
    }
}
