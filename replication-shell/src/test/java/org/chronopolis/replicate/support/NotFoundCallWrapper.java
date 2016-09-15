package org.chronopolis.replicate.support;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

/**
 * HTTP Call which simulates a 404 response
 *
 * Created by shake on 4/27/16.
 */
public class NotFoundCallWrapper<E> extends CallWrapper<E> {

    public NotFoundCallWrapper() {
        super(null);
    }

    @Override
    public Response<E> execute() throws IOException {
        return Response.error(404, ResponseBody.create(MediaType.parse("text/plain"), "test-sample-error"));
    }

    @Override
    public void enqueue(Callback<E> callback) {
        callback.onResponse(Response.error(404, ResponseBody.create(MediaType.parse("text/plain"), "test-sample-error")));
    }

}
