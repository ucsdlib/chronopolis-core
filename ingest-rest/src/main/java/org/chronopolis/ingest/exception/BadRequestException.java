package org.chronopolis.ingest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception caused by an HTTP 400
 *
 * Created by shake on 1/5/15.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    public BadRequestException(String s) {
        super(s);
    }

}
