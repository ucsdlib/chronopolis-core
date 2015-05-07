package org.chronopolis.common.dpn;

import retrofit.RequestInterceptor;

/**
 * Created by shake on 5/7/15.
 */
public class TokenInterceptor implements RequestInterceptor {

    private String token;

    public TokenInterceptor(String token) {
        this.token = token;
    }

    @Override
    public void intercept(RequestFacade requestFacade) {
        String tokenAuth = "token " + token;
        requestFacade.addHeader("Authorization", tokenAuth);

        requestFacade.addHeader("Accept", "*/*");
    }
}
