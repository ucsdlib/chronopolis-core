package org.chronopolis.rest.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.ErrorHandler;
import retrofit.RetrofitError;

/**
 * Retrofit {@link ErrorHandler} to log errors which happen from retrofit,
 * so that the reasons do not get lost in the stack trace
 *
 * Created by shake on 4/23/15.
 */
public class ErrorLogger implements ErrorHandler {
    private final Logger log = LoggerFactory.getLogger(ErrorLogger.class);

    @Override
    public Throwable handleError(RetrofitError retrofitError) {
        log.debug("Handling error from retrofit");

        String url = retrofitError.getUrl();
        int status = retrofitError.getResponse().getStatus();
        String reason = retrofitError.getResponse().getReason();
        log.error("Error in http call: url: {} status: {} reason: {}",new Object[]{url, status, reason});

        return retrofitError.getCause();
    }
}
