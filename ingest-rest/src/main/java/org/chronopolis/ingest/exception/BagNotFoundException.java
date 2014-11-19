package org.chronopolis.ingest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by shake on 11/19/14.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class BagNotFoundException extends RuntimeException {

    public BagNotFoundException(Long bagId) {
        super("Could not find bag " + bagId);
    }

}
