package vn.io.ajarline.administration.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import vn.io.ajarline.platform.web.ApiProblem;
import vn.io.ajarline.platform.web.ApiProblemWriter;

import java.util.Map;

import static java.util.Map.entry;

final class AdminFailureMapping {
    private static final Map<String, Problem> PROBLEMS = Map.ofEntries(
            entry("CONTENT_NOT_FOUND", new Problem(404, "Content was not found", "Content was not found.")),
            entry("VALIDATION_FAILED", new Problem(422, "Request validation failed", "One or more fields are invalid.")),
            entry("SLUG_CONFLICT", new Problem(409, "Slug conflicts", "Content with this slug already exists.")),
            entry("SLUG_IMMUTABLE_AFTER_PUBLICATION", new Problem(409, "Slug cannot be changed", "The slug cannot change after first publication.")),
            entry("REVISION_CONFLICT", new Problem(412, "Revision conflict", "The resource changed since it was loaded.")),
            entry("TOPIC_REFERENCE_NOT_FOUND", new Problem(422, "Topic reference was not found", "One or more Topic ids do not exist.")),
            entry("INACTIVE_TOPIC_ASSIGNMENT", new Problem(422, "Topic cannot be assigned", "An inactive Topic cannot be newly assigned.")),
            entry("CONTENT_SUMMARY_REQUIRED", new Problem(422, "Summary is required", "Published content must have a summary.")),
            entry("ARTICLE_BODY_REQUIRED", new Problem(422, "Body is required", "A published Article must have a body.")),
            entry("NEWS_BODY_REQUIRED", new Problem(422, "Body is required", "A published News item must have a body.")),
            entry("NEWS_SOURCE_REQUIRED", new Problem(422, "Source is required", "A published News item must have a source name and a valid source URL.")),
            entry("SERIES_OVERVIEW_REQUIRED", new Problem(422, "Overview is required", "A published Series must have an overview.")),
            entry("MODULE_OVERVIEW_REQUIRED", new Problem(422, "Overview is required", "A published Module must have an overview.")),
            entry("SERIES_REFERENCE_NOT_FOUND", new Problem(422, "Series reference was not found", "The Series does not exist.")),
            entry("MODULE_REFERENCE_NOT_FOUND", new Problem(422, "Module reference was not found", "The Module does not exist.")),
            entry("INVALID_POSITION", new Problem(422, "Position is invalid", "The position is outside the allowed range.")),
            entry("ORDER_SET_CONFLICT", new Problem(409, "Order does not match the children", "The ids must be exactly the current children.")),
            entry("INVALID_PUBLICATION_STATE", new Problem(409, "Content cannot be unpublished from the current state", "Content cannot be unpublished from the current state.")),
            entry("MODULE_HAS_NO_PUBLIC_ARTICLE", new Problem(422, "Module has no public Article", "A Module needs at least one published Article before it can be published.")),
            entry("SERIES_HAS_NO_PUBLIC_MODULE", new Problem(422, "Series has no public Module", "A Series needs at least one published Module with a published Article before it can be published.")),
            entry("CONTENT_ALREADY_EVER_PUBLISHED", new Problem(409, "Content cannot be hard-deleted in its current state", "Content cannot be hard-deleted in its current state."))
    );

    private AdminFailureMapping() {
    }

    static ResponseEntity<ApiProblem> response(ApiProblemWriter problems, HttpServletRequest request, String code) {
        Problem problem = PROBLEMS.get(code);
        if (problem == null) {
            return problems.response(request, 500, "INTERNAL_ERROR", "Unexpected internal failure", "Unexpected internal failure.");
        }
        return problems.response(request, problem.status(), code, problem.title(), problem.detail());
    }

    private record Problem(int status, String title, String detail) {
    }
}
