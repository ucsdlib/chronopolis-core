package org.chronopolis.rest.support;

import com.google.common.io.BaseEncoding;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Basic Auth interceptor for OkHttp
 *
 * Created by shake on 1/19/16.
 */
public class OkBasicInterceptor implements Interceptor {

    private final String username;
    private final String password;

    public OkBasicInterceptor(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String credentials = username + ":" + password;
        String basicAuth = "Basic " + BaseEncoding.base64()
                .encode(credentials.getBytes(StandardCharsets.UTF_8));

        Request request = chain.request().newBuilder().header("Authorization", basicAuth).build();

        return chain.proceed(request);
    }
}
