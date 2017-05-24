package org.chronopolis.ingest.models;

/**
 * Wrap an error from a bad request
 *
 * This is really only for displaying the error in the ui, nothing beyond that
 *
 * Created by shake on 5/24/17.
 */
public class HttpError {

    private final int code;
    private final String message;

    public HttpError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
