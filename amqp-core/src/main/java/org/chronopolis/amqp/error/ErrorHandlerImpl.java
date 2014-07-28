/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.amqp.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ErrorHandler;

/**
 *
 * @author shake
 */
public class ErrorHandlerImpl implements ErrorHandler {

    private final Logger log = LoggerFactory.getLogger(ErrorHandlerImpl.class);

    public void handleError(Throwable thrwbl) {
        log.error("Caught error: ", thrwbl);
    }

}
