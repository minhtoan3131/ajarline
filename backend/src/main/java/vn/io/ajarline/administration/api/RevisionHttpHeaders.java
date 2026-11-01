package vn.io.ajarline.administration.api;

final class RevisionHttpHeaders {

    private RevisionHttpHeaders() {

    }

    static String etag(long revision) {
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be at least 1");
        }
        return "\"" + revision + "\"";
    }
}
