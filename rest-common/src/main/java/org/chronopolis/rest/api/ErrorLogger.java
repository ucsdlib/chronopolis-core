package org.chronopolis.rest.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;

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

        // default throwable, if the error from retrofit is null
        Throwable cause = new Throwable("Retrofit error is null");

        if (retrofitError != null) {
            String url = retrofitError.getUrl();
            Response response = retrofitError.getResponse();
            if (response != null) {
                int status = response.getStatus();
                String reason = response.getReason();
                log.error("Error in http call: url: {} status: {} reason: {}", new Object[]{url, status, reason});
            } else {
                log.error("Error communicating with server; No Response");
            }
            cause = retrofitError.getCause();
        }

        return cause;
    }
}
