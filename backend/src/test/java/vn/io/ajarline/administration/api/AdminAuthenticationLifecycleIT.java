package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import vn.io.ajarline.support.AdminAuthenticationTestData;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgreSqlTestConfiguration.class,
        AbstractAdminAuthenticationIT.ProbeConfiguration.class})
class AdminAuthenticationLifecycleIT extends AbstractAdminAuthenticationIT {

    @Test
    void session_ShouldReportAnonymousState_WhenNoSessionExists() throws Exception {
        Browser browser = new Browser();

        HttpResponse<String> response = browser.get(AUTH + "/session");

        assertJson(response, 200, "{\"authenticated\":false}");
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
    }

    @Test
    void access_ShouldRequireSession_WhenRouteIsAdmin() throws Exception {
        Browser browser = new Browser();

        HttpResponse<String> publicResponse = browser.get("/api/v1/security-probe");
        HttpResponse<String> adminResponse = browser.get(ADMIN_PROBE);

        assertThat(publicResponse.statusCode()).isEqualTo(204);
        assertProblem(adminResponse, 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void login_ShouldRotateSessionAndReplaceCsrf_WhenCredentialsAreValid() throws Exception {
        Browser browser = new Browser();
        String oldToken = csrf(browser);
        String oldCookie = browser.cookie();

        HttpResponse<String> response = login(browser, oldToken,
                AdminAuthenticationTestData.username(),
                AdminAuthenticationTestData.password());

        assertJson(response, 200, "{\"authenticated\":true}");
        assertThat(browser.cookie()).isNotEqualTo(oldCookie);
        assertJson(browser.get(AUTH + "/session"), 200, "{\"authenticated\":true}");
        assertThat(browser.get(ADMIN_PROBE).statusCode()).isEqualTo(204);

        Browser oldSession = new Browser(oldCookie);
        assertProblem(oldSession.get(ADMIN_PROBE), 401, "AUTHENTICATION_REQUIRED");
        assertProblem(browser.post(ADMIN_PROBE, null, oldToken), 403, "CSRF_INVALID");
        assertProblem(browser.post(AUTH + "/logout", null, oldToken), 403, "CSRF_INVALID");
        assertThat(mutationCount()).isZero();

        String freshToken = csrf(browser);
        assertThat(browser.post(ADMIN_PROBE, null, freshToken).statusCode()).isEqualTo(204);
        assertThat(mutationCount()).isEqualTo(1);
    }

    @Test
    void logout_ShouldInvalidateSession_WhenCurrentCsrfIsValid() throws Exception {
        Browser browser = authenticatedBrowser();
        String token = csrf(browser);
        String authenticatedCookie = browser.cookie();

        HttpResponse<String> response = browser.post(AUTH + "/logout", null, token);

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
        assertNoStore(response);
        assertJson(browser.get(AUTH + "/session"), 200, "{\"authenticated\":false}");

        Browser replay = new Browser(authenticatedCookie);
        assertProblem(replay.get(ADMIN_PROBE), 401, "AUTHENTICATION_REQUIRED");
        assertProblem(replay.post(ADMIN_PROBE, null, token), 403, "CSRF_INVALID");
        assertThat(mutationCount()).isZero();
    }
}
