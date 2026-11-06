package vn.io.ajarline.administration.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.JsonNode;

public record AdminLoginRequest(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1,
                maxLength = USERNAME_MAX_LENGTH,
                description = "Configured Admin username. It must not be blank."
        )
        String username,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1,
                maxLength = PASSWORD_MAX_LENGTH,
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        String password
) {
    static final int USERNAME_MAX_LENGTH = 200;
    static final int PASSWORD_MAX_LENGTH = 1024;

    static AdminLoginRequest parse(JsonNode body) {
        if (body == null
                || !body.isObject()
                || body.size() != 2
                ||!isText(body.get("username"), 1, USERNAME_MAX_LENGTH)
                || body.get("username").asString().isBlank()
                || !isText(body.get("password"), 1, PASSWORD_MAX_LENGTH)) {
            throw new AuthController.InvalidAuthRequestException();
        }
        return new AdminLoginRequest(
                body.get("username").asString(),
                body.get("password").asString()
        );
    }

    private static boolean isText(JsonNode node, int min, int max) {
        if (node == null || !node.isString()) {
            return false;
        }

        String value = node.asString();
        int length = value.codePointCount(0, value.length());
        return length >= min && length <= max;
    }

    @Override
    public @NonNull String toString() {
        return "AdminLoginRequest[username=" + username + ", password=[REDACTED]]";
    }
}
