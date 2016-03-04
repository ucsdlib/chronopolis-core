package org.chronopolis.common.dpn;


/**
 * Created by shake on 5/7/15.
 */
@Deprecated
public class TokenInterceptor { //implements RequestInterceptor {

    private String token;

    public TokenInterceptor(String token) {
        this.token = token;
    }

    /*
    @Override
    public void intercept(RequestFacade requestFacade) {
        String tokenAuth = "Token token=" + token;
        requestFacade.addHeader("Authorization", tokenAuth);

        requestFacade.addHeader("Accept", "**");
    }
    */
}
