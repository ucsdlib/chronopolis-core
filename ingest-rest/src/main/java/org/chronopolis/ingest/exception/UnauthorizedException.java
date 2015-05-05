package org.chronopolis.ingest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception caused by HTTP 401
 *
 * Created by shake on 1/5/15.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String s) {
        super(s + " does not have authorization to access this resource");
    }

}
