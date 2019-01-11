package org.chronopolis.ingest.support;

import com.google.common.collect.ImmutableList;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

/**
 * Attempt at a generic result for use by a data access object
 *
 * @author shake
 */
public class QueryResult<T> {
    public enum Status {
        OK(ResponseEntity.ok()),
        CREATED(ResponseEntity.status(HttpStatus.CREATED)),
        CONFLICT(ResponseEntity.status(HttpStatus.CONFLICT)),
        FORBIDDEN(ResponseEntity.status(HttpStatus.FORBIDDEN)),
        NOT_FOUND(ResponseEntity.status(HttpStatus.NOT_FOUND)),
        BAD_REQUEST(ResponseEntity.badRequest());

        private final ResponseEntity.BodyBuilder builder;

        Status(ResponseEntity.BodyBuilder builder) {
            this.builder = builder;
        }
    }

    private T t;
    private Status status;
    private List<String> errors; // mostly for debugging

    public QueryResult(T t) {
        this.t = t;
        this.status = Status.CREATED;
        this.errors = ImmutableList.of();
    }

    public QueryResult(T t, Status status) {
        this.t = t;
        this.status = status;
        this.errors = ImmutableList.of();
    }

    public QueryResult(Status status, String error) {
        this.status = status;
        this.errors = ImmutableList.of(error);
    }

    public Optional<T> get() {
        return Optional.ofNullable(t);
    }

    public List<String> errors() {
        return errors;
    }

    public ResponseEntity<T> response() {
        if (t == null) {
            // how to embed the errors as part of the body?
            return status.builder.build();
        } else {
            return status.builder.body(t);
        }
    }

}
