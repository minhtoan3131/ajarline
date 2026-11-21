package vn.io.ajarline.administration.api;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Builds Concept request bodies and checks Concept response JSON for the Admin API tests.
 */
final class ConceptJsonFixture {

    private ConceptJsonFixture() {
    }

    static ObjectNode expression(String text, String languageHint, boolean autoExpand) {
        ObjectNode node = AbstractAdminAuthenticationIT.JSON.createObjectNode();
        node.put("expression", text);
        if (languageHint == null) {
            node.putNull("languageHint");
        } else {
            node.put("languageHint", languageHint);
        }
        node.put("autoExpand", autoExpand);
        return node;
    }

    /** Builds the body of a create request. */
    static String payload(
            String key, String displayName, String description, boolean active,
            ObjectNode... expressions) throws Exception {
        ObjectNode body = AbstractAdminAuthenticationIT.JSON.createObjectNode();
        body.put("key", key);
        body.put("displayName", displayName);
        putDescription(body, description);
        body.put("active", active);
        putExpressions(body, expressions);
        return AbstractAdminAuthenticationIT.JSON.writeValueAsString(body);
    }

    /** Builds the body of an update request. It has no key. */
    static String editPayload(
            String displayName, String description, boolean active,
            ObjectNode... expressions) throws Exception {
        ObjectNode body = AbstractAdminAuthenticationIT.JSON.createObjectNode();
        body.put("displayName", displayName);
        putDescription(body, description);
        body.put("active", active);
        putExpressions(body, expressions);
        return AbstractAdminAuthenticationIT.JSON.writeValueAsString(body);
    }

    static void assertItem(
            JsonNode item, String key, String displayName, String description, boolean active) {
        assertThat(item.propertyNames()).containsExactlyInAnyOrder(
                "id", "key", "displayName", "description", "active",
                "expressions", "createdAt", "updatedAt");
        assertThat(item.get("id").asString()).isNotBlank();
        assertThat(item.get("key").asString()).isEqualTo(key);
        assertThat(item.get("displayName").asString()).isEqualTo(displayName);
        if (description == null) {
            assertThat(item.get("description").isNull()).isTrue();
        } else {
            assertThat(item.get("description").asString()).isEqualTo(description);
        }
        assertThat(item.get("active").asBoolean()).isEqualTo(active);
        assertThat(item.get("createdAt").asString()).isNotBlank();
        assertThat(item.get("updatedAt").asString()).isNotBlank();
    }

    static void assertExpression(
            JsonNode item, String text, String languageHint, boolean autoExpand) {
        assertThat(item.propertyNames()).containsExactlyInAnyOrder(
                "expression", "languageHint", "autoExpand");
        assertThat(item.get("expression").asString()).isEqualTo(text);
        if (languageHint == null) {
            assertThat(item.get("languageHint").isNull()).isTrue();
        } else {
            assertThat(item.get("languageHint").asString()).isEqualTo(languageHint);
        }
        assertThat(item.get("autoExpand").asBoolean()).isEqualTo(autoExpand);
    }

    private static void putDescription(ObjectNode body, String description) {
        if (description == null) {
            body.putNull("description");
        } else {
            body.put("description", description);
        }
    }

    private static void putExpressions(ObjectNode body, ObjectNode... expressions) {
        ArrayNode array = body.putArray("expressions");
        for (ObjectNode expression : expressions) {
            array.add(expression);
        }
    }
}
