package org.chronopolis.ingest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception caused by an HTTP 409
 *
 * Created by shake on 1/5/15.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    public ConflictException() {
        super("Resource has already been accepted by another node");
    }


}
