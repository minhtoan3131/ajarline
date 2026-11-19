package vn.io.ajarline.administration.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import tools.jackson.databind.json.JsonMapper;
import vn.io.ajarline.content.contract.ContentFailureCode;
import vn.io.ajarline.platform.web.ApiProblem;
import vn.io.ajarline.platform.web.ApiProblemWriter;

import static org.assertj.core.api.Assertions.assertThat;

class AdminFailureMappingTest {

    private final ApiProblemWriter problems = new ApiProblemWriter(JsonMapper.builder().build());

    @ParameterizedTest(name = "[{index}] maps {0}")
    @EnumSource(ContentFailureCode.class)
    void response_ShouldNotBeInternalError_WhenContentFailureHasACode(ContentFailureCode code) {
        ResponseEntity<ApiProblem> response = map(code.name());

        assertThat(response.getStatusCode().value()).isNotEqualTo(500);
    }

    @ParameterizedTest(name = "[{index}] maps {0}")
    @EnumSource(value = ContentFailureCode.class, names = {"NEWS_BODY_REQUIRED", "NEWS_SOURCE_REQUIRED"})
    void response_ShouldBeUnprocessable_WhenNewsPartIsMissing(ContentFailureCode code) {
        ResponseEntity<ApiProblem> response = map(code.name());

        assertThat(response.getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void response_ShouldBeInternalError_WhenCodeIsUnknown() {
        ResponseEntity<ApiProblem> response = map("NOT_A_REAL_CODE");

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    private ResponseEntity<ApiProblem> map(String code) {
        return AdminFailureMapping.response(problems, new MockHttpServletRequest(), code);
    }
}
